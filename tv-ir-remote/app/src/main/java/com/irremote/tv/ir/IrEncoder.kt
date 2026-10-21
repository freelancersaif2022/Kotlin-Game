package com.irremote.tv.ir

/** A ready-to-send IR burst: carrier frequency in Hz and alternating mark/space durations in µs. */
class IrSignal(val frequency: Int, val pattern: IntArray)

/**
 * Turns [IrCode]s into mark/space patterns. Address/command semantics match Flipper Zero's
 * infrared library, which is the format of the bundled code database.
 */
object IrEncoder {

    fun encode(code: IrCode, toggle: Boolean = false): IrSignal? = when (code) {
        is IrCode.Raw -> IrSignal(code.frequency, evenLength(code.timings))
        is IrCode.Parsed -> encodeParsed(code, toggle)
    }

    fun encode(signal: String, toggle: Boolean = false): IrSignal? = IrCode.parse(signal)?.let { encode(it, toggle) }

    private fun encodeParsed(c: IrCode.Parsed, toggle: Boolean): IrSignal? {
        val a = c.address
        val cmd = c.command
        return when (c.protocol) {
            "NEC" -> nec(
                (a and 0xFF) or ((a.inv() and 0xFF) shl 8) or ((cmd and 0xFF) shl 16) or ((cmd.inv() and 0xFF) shl 24),
            )
            "NECX" -> nec((a and 0xFFFF) or ((cmd and 0xFFFF) shl 16))
            "S32" -> samsung32(
                (a and 0xFF) or ((a and 0xFF) shl 8) or ((cmd and 0xFF) shl 16) or ((cmd.inv() and 0xFF) shl 24),
            )
            "PIO" -> pioneer(
                (a and 0xFF) or ((a.inv() and 0xFF) shl 8) or ((cmd and 0xFF) shl 16) or ((cmd.inv() and 0xFF) shl 24),
            )
            "SIRC" -> sirc(cmd, a, 5)
            "SIRC15" -> sirc(cmd, a, 8)
            "SIRC20" -> sirc(cmd, a, 13)
            "RC5" -> rc5(a, cmd, extended = false, toggle = toggle)
            "RC5X" -> rc5(a, cmd, extended = true, toggle = toggle)
            "RC6" -> rc6(a, cmd, toggle)
            "KAS" -> kaseikyo(a, cmd)
            "RCA" -> rca(a, cmd)
            else -> null
        }
    }

    /** Collects marks and spaces, merging adjacent ones of the same kind. */
    private class Pulses {
        private val list = ArrayList<Int>(160)
        private var lastIsMark = false

        fun mark(us: Int) = add(true, us)
        fun space(us: Int) = add(false, us)

        private fun add(isMark: Boolean, us: Int) {
            if (list.isEmpty()) {
                if (!isMark) return // a pattern must start with a mark
                list.add(us)
            } else if (isMark == lastIsMark) {
                list[list.size - 1] = list[list.size - 1] + us
            } else {
                list.add(us)
            }
            lastIsMark = isMark
        }

        fun duration(): Int = list.sum()

        fun build(): IntArray = evenLength(list.toIntArray())
    }

    private fun evenLength(p: IntArray): IntArray =
        if (p.size % 2 == 0) p else p.copyOf(p.size + 1).also { it[p.size] = 20_000 }

    private fun Pulses.lsbBits(data: Long, count: Int, mark: Int, one: Int, zero: Int) {
        for (i in 0 until count) {
            mark(mark)
            space(if ((data shr i) and 1L == 1L) one else zero)
        }
    }

    private fun nec(data: Long): IrSignal {
        val p = Pulses()
        p.mark(9000); p.space(4500)
        p.lsbBits(data, 32, 560, 1690, 560)
        p.mark(560)
        p.space(108_000 - p.duration())
        return IrSignal(38_000, p.build())
    }

    private fun samsung32(data: Long): IrSignal {
        val p = Pulses()
        p.mark(4500); p.space(4500)
        p.lsbBits(data, 32, 550, 1650, 550)
        p.mark(550)
        p.space(108_000 - p.duration())
        return IrSignal(38_000, p.build())
    }

    private fun pioneer(data: Long): IrSignal {
        val p = Pulses()
        repeat(2) {
            p.mark(8500); p.space(4250)
            p.lsbBits(data, 32, 500, 1500, 500)
            p.mark(500)
            p.space(26_000)
        }
        return IrSignal(40_000, p.build())
    }

    /** Sony SIRC: 7 command bits then 5/8/13 address bits, LSB first, sent three times. */
    private fun sirc(command: Long, address: Long, addressBits: Int): IrSignal {
        val data = (command and 0x7F) or ((address and ((1L shl addressBits) - 1)) shl 7)
        val bits = 7 + addressBits
        val p = Pulses()
        repeat(3) {
            val start = p.duration()
            p.mark(2400); p.space(600)
            for (i in 0 until bits) {
                p.mark(if ((data shr i) and 1L == 1L) 1200 else 600)
                p.space(600)
            }
            p.space(maxOf(600, 45_000 - (p.duration() - start)))
        }
        return IrSignal(40_000, p.build())
    }

    /** Philips RC5 / RC5X, bi-phase: 1 = space→mark, 0 = mark→space. */
    private fun rc5(address: Long, command: Long, extended: Boolean, toggle: Boolean): IrSignal {
        val t = 889
        val field = if (extended) ((command shr 6) and 1L) == 0L else true
        val bits = ArrayList<Boolean>(14)
        bits.add(true)
        bits.add(field)
        bits.add(toggle)
        for (i in 4 downTo 0) bits.add((address shr i) and 1L == 1L)
        for (i in 5 downTo 0) bits.add((command shr i) and 1L == 1L)
        val p = Pulses()
        for (b in bits) {
            if (b) { p.space(t); p.mark(t) } else { p.mark(t); p.space(t) }
        }
        p.space(90_000)
        return IrSignal(36_000, p.build())
    }

    /** Philips RC6 mode 0: leader, start bit, 3 mode bits, double-width toggle, 8-bit address and command. */
    private fun rc6(address: Long, command: Long, toggle: Boolean): IrSignal {
        val t = 444
        val p = Pulses()
        p.mark(6 * t); p.space(2 * t)
        fun bit(one: Boolean, w: Int = t) { if (one) { p.mark(w); p.space(w) } else { p.space(w); p.mark(w) } }
        bit(true)
        repeat(3) { bit(false) }
        bit(toggle, 2 * t)
        for (i in 7 downTo 0) bit((address shr i) and 1L == 1L)
        for (i in 7 downTo 0) bit((command shr i) and 1L == 1L)
        p.space(90_000)
        return IrSignal(36_000, p.build())
    }

    /** Panasonic / Kaseikyo 48-bit frame. Address packs id(2) vendor(16) genre1(4) genre2(4). */
    private fun kaseikyo(address: Long, command: Long): IrSignal {
        val id = ((address shr 24) and 3).toInt()
        val vendor = ((address shr 8) and 0xFFFF).toInt()
        val genre1 = ((address shr 4) and 0xF).toInt()
        val genre2 = (address and 0xF).toInt()
        val cmd = (command and 0x3FF).toInt()
        var parity = vendor xor (vendor shr 8)
        parity = (parity xor (parity shr 4)) and 0xF
        val d = IntArray(6)
        d[0] = vendor and 0xFF
        d[1] = (vendor shr 8) and 0xFF
        d[2] = (parity or (genre1 shl 4)) and 0xFF
        d[3] = (genre2 or ((cmd and 0xF) shl 4)) and 0xFF
        d[4] = ((id shl 6) or (cmd shr 4)) and 0xFF
        d[5] = d[2] xor d[3] xor d[4]
        val p = Pulses()
        p.mark(3456); p.space(1728)
        for (byte in d) p.lsbBits(byte.toLong(), 8, 432, 1296, 432)
        p.mark(432)
        p.space(74_000)
        return IrSignal(37_000, p.build())
    }

    /** RCA: 4-bit address, 8-bit command, then both inverted, MSB first. */
    private fun rca(address: Long, command: Long): IrSignal {
        val p = Pulses()
        fun msb(v: Long, n: Int) {
            for (i in n - 1 downTo 0) {
                p.mark(500)
                p.space(if ((v shr i) and 1L == 1L) 2000 else 1000)
            }
        }
        p.mark(4000); p.space(4000)
        msb(address and 0xF, 4)
        msb(command and 0xFF, 8)
        msb(address.inv() and 0xF, 4)
        msb(command.inv() and 0xFF, 8)
        p.mark(500)
        p.space(16_000)
        return IrSignal(56_000, p.build())
    }
}
