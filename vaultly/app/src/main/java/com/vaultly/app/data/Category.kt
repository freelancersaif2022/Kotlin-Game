package com.vaultly.app.data

enum class Category(val label: String, val defaultWarrantyMonths: Int?) {
    ELECTRONICS("Electronics", 12),
    APPLIANCES("Appliances", 24),
    FASHION("Fashion", null),
    HOME("Home & Living", null),
    BEAUTY("Beauty", null),
    SPORTS("Sports", 6),
    TOOLS("Tools", 12),
    TOYS("Kids & Toys", null),
    GROCERY("Grocery", null),
    OTHER("Other", null);

    companion object {
        fun fromName(name: String?): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}
