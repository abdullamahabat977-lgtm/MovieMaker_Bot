package com.moviestudio.filmmaker

/**
 * Curated starter catalogue for the four first Movie Studio asset groups.
 * These are catalogue definitions, not bundled 3D mesh files yet.
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
        // Characters — 20
        MovieAssetEntry("character_man_adult", CHARACTERS, "عادي نارینه", "د ورځني ژوند لپاره ساده بالغ نارینه کرکټر؛ د جامو او رنګ بدلولو لپاره مناسب."),
        MovieAssetEntry("character_woman_adult", CHARACTERS, "عادي ښځه", "د ورځني ژوند ساده بالغ ښځینه کرکټر؛ د بېلابېلو صحنو لپاره."),
        MovieAssetEntry("character_boy", CHARACTERS, "هلک ماشوم", "د ماشومانو د کیسو او کورنیو صحنو لپاره کرکټر."),
        MovieAssetEntry("character_girl", CHARACTERS, "نجلۍ ماشومه", "د ماشومانو د کیسو او کورنیو صحنو لپاره کرکټر."),
        MovieAssetEntry("character_elder_man", CHARACTERS, "سپین ږیری", "د سپین ږیري څېره او ساده لباس؛ د کورنۍ او کلي صحنې."),
        MovieAssetEntry("character_elder_woman", CHARACTERS, "سپین سرې", "د سپین سرې څېره او ساده لباس؛ د کورنۍ او کلي صحنې."),
        MovieAssetEntry("character_king", CHARACTERS, "پاچا", "تاج او شاهي جامې لرونکی کرکټر؛ د ماڼۍ او تاریخي کیسو لپاره."),
        MovieAssetEntry("character_queen", CHARACTERS, "ملکه", "شاهي لباس او تاج لرونکی کرکټر؛ د ماڼۍ صحنو لپاره."),
        MovieAssetEntry("character_princess", CHARACTERS, "شهزادګۍ", "د شاهي کیسو لپاره ځوان ښځینه کرکټر."),
        MovieAssetEntry("character_police", CHARACTERS, "پولیس", "یونیفورم لرونکی پولیس؛ د ښار او نظم صحنو لپاره."),
        MovieAssetEntry("character_soldier", CHARACTERS, "سرتیری", "ساده پوځي یونیفورم لرونکی کرکټر؛ د تاریخي او خیالي صحنو لپاره."),
        MovieAssetEntry("character_guard", CHARACTERS, "ساتونکی", "د دروازې یا ماڼۍ ساتونکی؛ د ساتنې صحنو لپاره."),
        MovieAssetEntry("character_doctor", CHARACTERS, "ډاکټر", "د طبي لباس لرونکی کرکټر؛ د روغتون او درملنې صحنو لپاره."),
        MovieAssetEntry("character_nurse", CHARACTERS, "نرس", "د نرسۍ لباس لرونکی کرکټر؛ د روغتیايي صحنو لپاره."),
        MovieAssetEntry("character_teacher", CHARACTERS, "ښوونکی", "د ټولګي او ښوونځي صحنو لپاره کرکټر."),
        MovieAssetEntry("character_farmer", CHARACTERS, "بزګر", "د کروندې او کلي د ژوند لپاره کرکټر."),
        MovieAssetEntry("character_worker", CHARACTERS, "کارګر", "د ودانیزو او ورځنیو کارونو لپاره کرکټر."),
        MovieAssetEntry("character_engineer", CHARACTERS, "انجنیر", "د ساختماني او تخنیکي صحنو لپاره کرکټر."),
        MovieAssetEntry("character_hero", CHARACTERS, "اتل", "د کیسې اصلي اتل؛ د ځانګړو جامو او رنګونو لپاره مناسب."),
        MovieAssetEntry("character_merchant", CHARACTERS, "سوداګر", "د بازار او دوکان صحنو لپاره کرکټر."),

        // Creatures — 20
        MovieAssetEntry("creature_lion", CREATURES, "زمری", "د ځنګلي ژوند لپاره څلور پښې لرونکی لوی ښکارکوونکی حیوان."),
        MovieAssetEntry("creature_tiger", CREATURES, "پړانګ", "د پټو لرونکی ځنګلي حیوان؛ د ځنګل صحنو لپاره."),
        MovieAssetEntry("creature_wolf", CREATURES, "لېوه", "د ځنګلي ډلې او غرنیو صحنو لپاره حیوان."),
        MovieAssetEntry("creature_fox", CREATURES, "ګیدړ", "کوچنی ځیرک ځنګلي حیوان؛ د کیسو لپاره."),
        MovieAssetEntry("creature_dog", CREATURES, "سپی", "د کور او کلي د صحنو لپاره کورنی حیوان."),
        MovieAssetEntry("creature_cat", CREATURES, "پیشو", "د کور دننه او انګړ صحنو لپاره کورنی حیوان."),
        MovieAssetEntry("creature_horse", CREATURES, "آس", "د سفر، کلي او تاریخي صحنو لپاره آس."),
        MovieAssetEntry("creature_camel", CREATURES, "اوښ", "د دښتې او کاروان صحنو لپاره اوښ."),
        MovieAssetEntry("creature_cow", CREATURES, "غوا", "د فارم او کلي د ژوند لپاره کورنی حیوان."),
        MovieAssetEntry("creature_sheep", CREATURES, "پسه", "د څړځای او رمې صحنو لپاره حیوان."),
        MovieAssetEntry("creature_goat", CREATURES, "وزه", "د غره او کلي د صحنو لپاره حیوان."),
        MovieAssetEntry("creature_elephant", CREATURES, "فیل", "لوی ځنګلي حیوان؛ د طبیعي او خیالي صحنو لپاره."),
        MovieAssetEntry("creature_deer", CREATURES, "هوسۍ", "د ځنګل او شنو سیمو لپاره نازک حیوان."),
        MovieAssetEntry("creature_rabbit", CREATURES, "سوی", "کوچنی حیوان؛ د باغ او ځنګل صحنو لپاره."),
        MovieAssetEntry("creature_monkey", CREATURES, "بیزو", "د ونو او ځنګل صحنو لپاره خزندونکی حیوان."),
        MovieAssetEntry("creature_snake", CREATURES, "مار", "د ځمکې پر سر خوځېدونکی خزنده؛ د طبیعي صحنو لپاره."),
        MovieAssetEntry("creature_eagle", CREATURES, "عقاب", "د غرونو او اسمان د صحنو لپاره لوی ښکارکوونکی مرغه."),
        MovieAssetEntry("creature_parrot", CREATURES, "طوطي", "رنګین مرغه؛ د کور او ځنګل صحنو لپاره."),
        MovieAssetEntry("creature_chicken", CREATURES, "چرګه", "د فارم او کلي د صحنو لپاره کورنی مرغه."),
        MovieAssetEntry("creature_dragon", CREATURES, "خیالي ډریگن", "د افسانوي کیسو لپاره وزر لرونکی خیالي موجود."),

        // Environments — 20
        MovieAssetEntry("environment_green_forest", ENVIRONMENTS, "شنه ځنګل", "لوړې ونې، شنه واښه او طبیعي بوټي؛ د ورځې رڼا."),
        MovieAssetEntry("environment_dry_forest", ENVIRONMENTS, "وچ ځنګل", "وچې ونې، لږ بوټي او د خاورې ځمکه."),
        MovieAssetEntry("environment_snow_forest", ENVIRONMENTS, "واورین ځنګل", "د واورې پوښلې ونې او سړه ژمنۍ فضا."),
        MovieAssetEntry("environment_mountains", ENVIRONMENTS, "لوړ غرونه", "د ډبرو څوکې او لرې غرنی منظر."),
        MovieAssetEntry("environment_snow_mountains", ENVIRONMENTS, "واورین غرونه", "د واورې پوښلې څوکې او یخه فضا."),
        MovieAssetEntry("environment_desert", ENVIRONMENTS, "د شګو دښته", "د شګو غونډۍ او پراخه وچه سیمه."),
        MovieAssetEntry("environment_rocky_desert", ENVIRONMENTS, "ډبرینه دښته", "ډبرې، وچې غونډۍ او لږ نباتات."),
        MovieAssetEntry("environment_grassland", ENVIRONMENTS, "شنه څړځای", "هواره شنه ځمکه او واښه؛ د څارویو لپاره."),
        MovieAssetEntry("environment_river", ENVIRONMENTS, "سیند", "د اوبو بهېدونکې لاره او شنې غاړې."),
        MovieAssetEntry("environment_lake", ENVIRONMENTS, "جهیل", "ارامې اوبه او د جهیل شاوخوا طبیعي منظره."),
        MovieAssetEntry("environment_waterfall", ENVIRONMENTS, "آبشار", "له لوړې ډبرې څخه د اوبو راپرېوتل."),
        MovieAssetEntry("environment_ocean", ENVIRONMENTS, "سمندر", "پراخې نیلي اوبه او د افق کرښه."),
        MovieAssetEntry("environment_beach", ENVIRONMENTS, "ساحل", "شګه، د سمندر غاړه او د ساحل فضا."),
        MovieAssetEntry("environment_cave", ENVIRONMENTS, "سمڅه", "ډبرینه تیاره سمڅه؛ د سپړنې او راز صحنو لپاره."),
        MovieAssetEntry("environment_valley", ENVIRONMENTS, "دره", "د دوو غرونو ترمنځ شنه یا ډبرینه سیمه."),
        MovieAssetEntry("environment_village", ENVIRONMENTS, "کلی", "د کورونو، لارو او انګړونو لرونکی کلیوالي چاپېریال."),
        MovieAssetEntry("environment_city", ENVIRONMENTS, "ښار", "د سړکونو او ودانیو لرونکی ښاري چاپېریال."),
        MovieAssetEntry("environment_historic", ENVIRONMENTS, "تاریخي سیمه", "د پخوانیو ودانیو او ډبرو لارو لرونکی منظر."),
        MovieAssetEntry("environment_night", ENVIRONMENTS, "د شپې منظره", "تیاره اسمان او د شپې د رڼا لپاره مناسب چاپېریال."),
        MovieAssetEntry("environment_rain_snow", ENVIRONMENTS, "باران او واوره", "د هوا د بدلون لپاره د باران یا واورې صحنه."),

        // Buildings and ground — 20
        MovieAssetEntry("building_simple_house", BUILDINGS, "ساده کور", "یو پوړیز ساده کور؛ د کلي او عادي ژوند لپاره."),
        MovieAssetEntry("building_village_house", BUILDINGS, "کلیوالي کور", "د خټو یا ساده خښتو بڼه لرونکی کلیوالي کور."),
        MovieAssetEntry("building_modern_house", BUILDINGS, "عصري کور", "د عصري بڼې کور؛ کړکۍ او دروازه لري."),
        MovieAssetEntry("building_two_story_house", BUILDINGS, "دوه پوړیز کور", "د دوو پوړونو کور؛ د ښار او کلي لپاره."),
        MovieAssetEntry("building_apartment", BUILDINGS, "څو پوړیزه ودانۍ", "د استوګنې څو پوړیز بلاک."),
        MovieAssetEntry("building_palace", BUILDINGS, "ماڼۍ", "لویه شاهي ودانۍ؛ د تاریخي کیسو لپاره."),
        MovieAssetEntry("building_castle", BUILDINGS, "کلا", "د برجونو او دېوالونو لرونکې دفاعي ودانۍ."),
        MovieAssetEntry("building_school", BUILDINGS, "ښوونځی", "د ټولګیو او زده‌کړې د صحنو لپاره ودانۍ."),
        MovieAssetEntry("building_hospital", BUILDINGS, "روغتون", "د روغتیايي او بیړنیو صحنو لپاره ودانۍ."),
        MovieAssetEntry("building_mosque", BUILDINGS, "جومات", "د عبادت ځای؛ ساده معماري او مناره."),
        MovieAssetEntry("building_shop", BUILDINGS, "دوکان", "کوچنی دوکان؛ د بازار او پېرود صحنو لپاره."),
        MovieAssetEntry("building_market", BUILDINGS, "بازار", "د څو دوکانونو او تګ راتګ لرونکې سیمه."),
        MovieAssetEntry("building_office", BUILDINGS, "دفتر", "د کار او اداري صحنو لپاره ودانۍ."),
        MovieAssetEntry("building_hotel", BUILDINGS, "هوټل", "د مسافرو د استوګنې لپاره څو خونیزه ودانۍ."),
        MovieAssetEntry("building_factory", BUILDINGS, "فابریکه", "د تولید او صنعتي صحنو لپاره ودانۍ."),
        MovieAssetEntry("building_bridge", BUILDINGS, "پل", "د سیند یا لارې د تېرېدو لپاره پل."),
        MovieAssetEntry("building_courtyard", BUILDINGS, "انګړ", "د کور یا ودانۍ مخې ته خلاصه ساحه."),
        MovieAssetEntry("ground_grass", BUILDINGS, "شنه ځمکه", "د واښو پوښ لرونکې هواره ځمکه."),
        MovieAssetEntry("ground_sand", BUILDINGS, "شګلنه ځمکه", "د شګو سطحه؛ د دښتې او ساحل لپاره."),
        MovieAssetEntry("ground_rock_snow", BUILDINGS, "ډبرینه او واورینه ځمکه", "د ډبرو یا واورې سطحه؛ د غرونو او ژمي لپاره.")
    )

    fun forCategory(categoryId: String): List<MovieAssetEntry> =
        entries.filter { it.categoryId == categoryId }

    fun categoryIdForTitle(title: String): String? = when {
        title.contains("کرکټرونه") -> CHARACTERS
        title.contains("موجودات") -> CREATURES
        title.contains("چاپېریالونه") -> ENVIRONMENTS
        title.contains("ودانۍ") -> BUILDINGS
        else -> null
    }
}
