package com.moviestudio.filmmaker

/**
 * Curated local catalogue of five selected 2D asset entries per category.
 * Image files are loaded separately from app/src/main/assets/movie_assets/.
 */
data class MovieAssetEntry(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String
)

object MovieAssetLibrary {
    const val CHARACTERS = "characters"
    const val CREATURES = "creatures"
    const val ENVIRONMENTS = "environments"
    const val BUILDINGS = "buildings"

    val entries: List<MovieAssetEntry> = listOf(
        // فرنیچر — 20 جلا فلمي توکي
        MovieAssetEntry("furniture_01", "furniture", "د لرګي څوکۍ", "د لرګي څوکۍ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("furniture_02", "furniture", "عصري څوکۍ", "عصري څوکۍ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("furniture_03", "furniture", "دفترۍ څوکۍ", "دفترۍ څوکۍ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("furniture_04", "furniture", "دوه کسیزه صوفه", "دوه کسیزه صوفه؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("furniture_05", "furniture", "درې کسیزه صوفه", "درې کسیزه صوفه؛ د فلمي صحنو لپاره جلا توکی."),
        // موټرونه — 20 جلا فلمي توکي
        MovieAssetEntry("vehicles_01", "vehicles", "کوچنی ښاري موټر", "کوچنی ښاري موټر؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("vehicles_02", "vehicles", "لوکس سیډان", "لوکس سیډان؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("vehicles_03", "vehicles", "سپورتي موټر", "سپورتي موټر؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("vehicles_04", "vehicles", "کلاسیک موټر", "کلاسیک موټر؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("vehicles_05", "vehicles", "برقي موټر", "برقي موټر؛ د فلمي صحنو لپاره جلا توکی."),
        // جامې — 20 جلا فلمي توکي
        MovieAssetEntry("clothes_01", "clothes", "د نارینه کمیس", "د نارینه کمیس؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("clothes_02", "clothes", "د ښځو اوږد لباس", "د ښځو اوږد لباس؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("clothes_03", "clothes", "د ماشومانو کالي", "د ماشومانو کالي؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("clothes_04", "clothes", "دودیزه افغاني جامه", "دودیزه افغاني جامه؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("clothes_05", "clothes", "پوځي یونیفورم", "پوځي یونیفورم؛ د فلمي صحنو لپاره جلا توکی."),
        // وسلې — 20 جلا فلمي توکي
        MovieAssetEntry("weapons_01", "weapons", "تاریخي توره", "تاریخي توره؛ د فلمي صحنو لپاره جلا نمایشي ماډل."),
        MovieAssetEntry("weapons_02", "weapons", "لنډ خنجر", "لنډ خنجر؛ د فلمي صحنو لپاره جلا نمایشي ماډل."),
        MovieAssetEntry("weapons_03", "weapons", "اوږده نېزه", "اوږده نېزه؛ د فلمي صحنو لپاره جلا نمایشي ماډل."),
        MovieAssetEntry("weapons_04", "weapons", "تیر او لینده", "تیر او لینده؛ د فلمي صحنو لپاره جلا نمایشي ماډل."),
        MovieAssetEntry("weapons_05", "weapons", "کمان", "کمان؛ د فلمي صحنو لپاره جلا نمایشي ماډل."),
        // حرکتونه — 20 جلا فلمي توکي
        MovieAssetEntry("motions_01", "motions", "عادي قدم وهل", "عادي قدم وهل؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("motions_02", "motions", "چټک قدم وهل", "چټک قدم وهل؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("motions_03", "motions", "منډه وهل", "منډه وهل؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("motions_04", "motions", "ورو منډه", "ورو منډه؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("motions_05", "motions", "لوړ ټوپ", "لوړ ټوپ؛ د فلمي صحنو لپاره جلا توکی."),
        // احساسات — 20 جلا فلمي توکي
        MovieAssetEntry("emotions_01", "emotions", "خوښي", "خوښي؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("emotions_02", "emotions", "لویه خندا", "لویه خندا؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("emotions_03", "emotions", "نرمه موسکا", "نرمه موسکا؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("emotions_04", "emotions", "خفګان", "خفګان؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("emotions_05", "emotions", "ژړا", "ژړا؛ د فلمي صحنو لپاره جلا توکی."),
        // کمرې — 20 جلا فلمي توکي
        MovieAssetEntry("cameras_01", "cameras", "عمومي شاټ", "عمومي شاټ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("cameras_02", "cameras", "منځنی شاټ", "منځنی شاټ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("cameras_03", "cameras", "نږدې شاټ", "نږدې شاټ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("cameras_04", "cameras", "ډېر نږدې شاټ", "ډېر نږدې شاټ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("cameras_05", "cameras", "له پاسه لید", "له پاسه لید؛ د فلمي صحنو لپاره جلا توکی."),
        // غږونه او موسیقي — 20 جلا فلمي توکي
        MovieAssetEntry("sounds_01", "sounds", "د باد غږ", "د باد غږ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("sounds_02", "sounds", "د باران غږ", "د باران غږ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("sounds_03", "sounds", "د تندر غږ", "د تندر غږ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("sounds_04", "sounds", "د سیند غږ", "د سیند غږ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("sounds_05", "sounds", "د سمندر څپې", "د سمندر څپې؛ د فلمي صحنو لپاره جلا توکی."),
        // مخابرې او ټیلیفونونه — 20 جلا فلمي توکي
        MovieAssetEntry("phones_01", "phones", "زوړ ګرځنده ټیلیفون", "زوړ ګرځنده ټیلیفون؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("phones_02", "phones", "عصري سمارټ فون", "عصري سمارټ فون؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("phones_03", "phones", "تڼۍ لرونکی موبایل", "تڼۍ لرونکی موبایل؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("phones_04", "phones", "دفتر ټیلیفون", "دفتر ټیلیفون؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("phones_05", "phones", "بېسیم ټیلیفون", "بېسیم ټیلیفون؛ د فلمي صحنو لپاره جلا توکی."),
        // د کار وسایل — 20 جلا فلمي توکي
        MovieAssetEntry("tools_01", "tools", "هامر", "هامر؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("tools_02", "tools", "پیچ‌کش", "پیچ‌کش؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("tools_03", "tools", "انبردست", "انبردست؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("tools_04", "tools", "اره", "اره؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("tools_05", "tools", "لاس برمه", "لاس برمه؛ د فلمي صحنو لپاره جلا توکی."),
        // طبي وسایل — 20 جلا فلمي توکي
        MovieAssetEntry("medical_01", "medical", "سټېتوسکوپ", "سټېتوسکوپ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("medical_02", "medical", "د فشار ماشین", "د فشار ماشین؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("medical_03", "medical", "ترمامیتر", "ترمامیتر؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("medical_04", "medical", "د اکسیجن ماسک", "د اکسیجن ماسک؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("medical_05", "medical", "ویلچر", "ویلچر؛ د فلمي صحنو لپاره جلا توکی."),
        // خوراک او څښاک — 20 جلا فلمي توکي
        MovieAssetEntry("food_01", "food", "مڼه", "مڼه؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("food_02", "food", "کېله", "کېله؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("food_03", "food", "مالټه", "مالټه؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("food_04", "food", "انګور", "انګور؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("food_05", "food", "انار", "انار؛ د فلمي صحنو لپاره جلا توکی."),
        // د دفتر توکي — 20 جلا فلمي توکي
        MovieAssetEntry("office_01", "office", "لپټاپ", "لپټاپ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("office_02", "office", "ډېسکټاپ کمپیوټر", "ډېسکټاپ کمپیوټر؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("office_03", "office", "کمپیوټر سکرین", "کمپیوټر سکرین؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("office_04", "office", "کیبورډ", "کیبورډ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("office_05", "office", "ماوس", "ماوس؛ د فلمي صحنو لپاره جلا توکی."),
        // سپورټ او لوبې — 20 جلا فلمي توکي
        MovieAssetEntry("sports_01", "sports", "د فوټبال توپ", "د فوټبال توپ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("sports_02", "sports", "د باسکټبال توپ", "د باسکټبال توپ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("sports_03", "sports", "د والیبال توپ", "د والیبال توپ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("sports_04", "sports", "د کرکټ توپ", "د کرکټ توپ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("sports_05", "sports", "د کرکټ بېټ", "د کرکټ بېټ؛ د فلمي صحنو لپاره جلا توکی."),
        // د سفر وسایل — 20 جلا فلمي توکي
        MovieAssetEntry("travel_01", "travel", "لوی سفري بکس", "لوی سفري بکس؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("travel_02", "travel", "لاسي بکس", "لاسي بکس؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("travel_03", "travel", "د سفر کڅوړه", "د سفر کڅوړه؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("travel_04", "travel", "د غر ختلو بکس", "د غر ختلو بکس؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("travel_05", "travel", "کمپ خېمه", "کمپ خېمه؛ د فلمي صحنو لپاره جلا توکی."),
        // د سړک توکي — 20 جلا فلمي توکي
        MovieAssetEntry("road_01", "road", "د تم کېدو نښه", "د تم کېدو نښه؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("road_02", "road", "د سرعت محدودیت نښه", "د سرعت محدودیت نښه؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("road_03", "road", "د ترافیک څراغ", "د ترافیک څراغ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("road_04", "road", "د سړک څراغ", "د سړک څراغ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("road_05", "road", "د سړک خنډ", "د سړک خنډ؛ د فلمي صحنو لپاره جلا توکی."),
        // برېښنايي توکي — 20 جلا فلمي توکي
        MovieAssetEntry("electronics_01", "electronics", "د مېز څراغ", "د مېز څراغ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("electronics_02", "electronics", "ولاړ څراغ", "ولاړ څراغ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("electronics_03", "electronics", "چت څراغ", "چت څراغ؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("electronics_04", "electronics", "تلویزیون", "تلویزیون؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("electronics_05", "electronics", "سپیکر", "سپیکر؛ د فلمي صحنو لپاره جلا توکی."),
        // نور فلمي توکي — 20 جلا فلمي توکي
        MovieAssetEntry("other_01", "other", "د لرګي صندوق", "د لرګي صندوق؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("other_02", "other", "فلزي صندوق", "فلزي صندوق؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("other_03", "other", "د خزانې صندوق", "د خزانې صندوق؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("other_04", "other", "د ګلدان لوښی", "د ګلدان لوښی؛ د فلمي صحنو لپاره جلا توکی."),
        MovieAssetEntry("other_05", "other", "د ګلانو ګلدسته", "د ګلانو ګلدسته؛ د فلمي صحنو لپاره جلا توکی."),
        // Characters — 20
        MovieAssetEntry("character_man_adult", CHARACTERS, "عادي نارینه", "د ورځني ژوند لپاره ساده بالغ نارینه کرکټر؛ د جامو او رنګ بدلولو لپاره مناسب."),
        MovieAssetEntry("character_woman_adult", CHARACTERS, "عادي ښځه", "د ورځني ژوند ساده بالغ ښځینه کرکټر؛ د بېلابېلو صحنو لپاره."),
        MovieAssetEntry("character_boy", CHARACTERS, "هلک ماشوم", "د ماشومانو د کیسو او کورنیو صحنو لپاره کرکټر."),
        MovieAssetEntry("character_girl", CHARACTERS, "نجلۍ ماشومه", "د ماشومانو د کیسو او کورنیو صحنو لپاره کرکټر."),
        MovieAssetEntry("character_elder_man", CHARACTERS, "سپین ږیری", "د سپین ږیري څېره او ساده لباس؛ د کورنۍ او کلي صحنې."),

        // Creatures — 20
        MovieAssetEntry("creature_lion", CREATURES, "زمری", "د ځنګلي ژوند لپاره څلور پښې لرونکی لوی ښکارکوونکی حیوان."),
        MovieAssetEntry("creature_tiger", CREATURES, "پړانګ", "د پټو لرونکی ځنګلي حیوان؛ د ځنګل صحنو لپاره."),
        MovieAssetEntry("creature_wolf", CREATURES, "لېوه", "د ځنګلي ډلې او غرنیو صحنو لپاره حیوان."),
        MovieAssetEntry("creature_fox", CREATURES, "ګیدړ", "کوچنی ځیرک ځنګلي حیوان؛ د کیسو لپاره."),
        MovieAssetEntry("creature_dog", CREATURES, "سپی", "د کور او کلي د صحنو لپاره کورنی حیوان."),

        // Environments — 20
        MovieAssetEntry("environment_green_forest", ENVIRONMENTS, "شنه ځنګل", "لوړې ونې، شنه واښه او طبیعي بوټي؛ د ورځې رڼا."),
        MovieAssetEntry("environment_dry_forest", ENVIRONMENTS, "وچ ځنګل", "وچې ونې، لږ بوټي او د خاورې ځمکه."),
        MovieAssetEntry("environment_snow_forest", ENVIRONMENTS, "واورین ځنګل", "د واورې پوښلې ونې او سړه ژمنۍ فضا."),
        MovieAssetEntry("environment_mountains", ENVIRONMENTS, "لوړ غرونه", "د ډبرو څوکې او لرې غرنی منظر."),
        MovieAssetEntry("environment_snow_mountains", ENVIRONMENTS, "واورین غرونه", "د واورې پوښلې څوکې او یخه فضا."),

        // Buildings and ground — 20
        MovieAssetEntry("building_simple_house", BUILDINGS, "ساده کور", "یو پوړیز ساده کور؛ د کلي او عادي ژوند لپاره."),
        MovieAssetEntry("building_village_house", BUILDINGS, "کلیوالي کور", "د خټو یا ساده خښتو بڼه لرونکی کلیوالي کور."),
        MovieAssetEntry("building_modern_house", BUILDINGS, "عصري کور", "د عصري بڼې کور؛ کړکۍ او دروازه لري."),
        MovieAssetEntry("building_two_story_house", BUILDINGS, "دوه پوړیز کور", "د دوو پوړونو کور؛ د ښار او کلي لپاره."),
        MovieAssetEntry("building_apartment", BUILDINGS, "څو پوړیزه ودانۍ", "د استوګنې څو پوړیز بلاک."),
    )

    fun forCategory(categoryId: String): List<MovieAssetEntry> =
        entries.filter { it.categoryId == categoryId }

    fun categoryIdForTitle(title: String): String? = when {
        title.contains("کرکټرونه") -> CHARACTERS
        title.contains("موجودات") -> CREATURES
        title.contains("چاپېریالونه") -> ENVIRONMENTS
        title.contains("ودانۍ") -> BUILDINGS
        title.contains("فرنیچر") -> "furniture"
        title.contains("موټرونه") -> "vehicles"
        title.contains("جامې") -> "clothes"
        title.contains("وسلې") -> "weapons"
        title.contains("حرکتونه") -> "motions"
        title.contains("احساسات") -> "emotions"
        title.contains("کمرې") -> "cameras"
        title.contains("غږونه او موسیقي") -> "sounds"
        title.contains("مخابرې او ټیلیفونونه") -> "phones"
        title.contains("د کار وسایل") -> "tools"
        title.contains("طبي وسایل") -> "medical"
        title.contains("خوراک او څښاک") -> "food"
        title.contains("د دفتر توکي") -> "office"
        title.contains("سپورټ او لوبې") -> "sports"
        title.contains("د سفر وسایل") -> "travel"
        title.contains("د سړک توکي") -> "road"
        title.contains("برېښنايي توکي") -> "electronics"
        title.contains("نور فلمي توکي") -> "other"
        else -> null
    }
}
