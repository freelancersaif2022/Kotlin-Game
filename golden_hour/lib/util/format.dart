import 'package:flutter/widgets.dart';

const _months = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];
const _weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

String two(int v) => v.toString().padLeft(2, '0');

bool use24h(BuildContext context) =>
    MediaQuery.maybeAlwaysUse24HourFormatOf(context) ?? false;

/// "18:42" or "6:42 PM" depending on the device setting.
String fmtTime(BuildContext context, DateTime? t) {
  if (t == null) return '—';
  if (use24h(context)) return '${two(t.hour)}:${two(t.minute)}';
  final h = t.hour % 12 == 0 ? 12 : t.hour % 12;
  return '$h:${two(t.minute)} ${t.hour < 12 ? 'AM' : 'PM'}';
}

/// "01:42:18" countdown.
String fmtCountdown(Duration d) {
  if (d.isNegative) d = Duration.zero;
  final h = d.inHours, m = d.inMinutes % 60, s = d.inSeconds % 60;
  return h > 0 ? '${two(h)}:${two(m)}:${two(s)}' : '${two(m)}:${two(s)}';
}

/// "1h 12m" / "38m".
String fmtDuration(Duration? d) {
  if (d == null) return '—';
  final h = d.inHours, m = d.inMinutes % 60;
  if (h == 0) return '${m}m';
  return '${h}h ${two(m)}m';
}

String fmtDate(DateTime d) =>
    '${_weekdays[d.weekday - 1]}, ${d.day} ${_months[d.month - 1]}';
String fmtDateLong(DateTime d) =>
    '${_weekdays[d.weekday - 1]}, ${d.day} ${_months[d.month - 1]} ${d.year}';
String weekday(DateTime d) => _weekdays[d.weekday - 1];
String month(DateTime d) => _months[d.month - 1];

String cardinal(double bearing) {
  const names = [
    'N',
    'NNE',
    'NE',
    'ENE',
    'E',
    'ESE',
    'SE',
    'SSE',
    'S',
    'SSW',
    'SW',
    'WSW',
    'W',
    'WNW',
    'NW',
    'NNW',
  ];
  return names[((bearing % 360) / 22.5 + 0.5).floor() % 16];
}

String fmtBearing(double b) => '${b.round() % 360}° ${cardinal(b)}';

bool sameDay(DateTime a, DateTime b) =>
    a.year == b.year && a.month == b.month && a.day == b.day;
