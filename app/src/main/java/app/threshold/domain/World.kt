package app.threshold.domain

/** A line in both languages. */
data class L(val en: String, val ru: String) {
    fun of(ru: Boolean) = if (ru) this.ru else en
}

/** The shape a find is drawn with. */
enum class Shape { FEATHER, DISC, STONE, BULB, TICKET, LEAF, KEY, STAR, CRYSTAL, SHELL, FLAG, BOTTLE, DROP, CIRCLE }

data class Resident(val name: L, val level: Int, val line: L)

/** The worlds behind the portal. Each is one kind of morning; the user picks which world a day opens onto. */
enum class World(val accent: Long, val title: L, val hint: L, val about: L, val shape: Shape, val residents: List<Resident>) {
    NEWS(
        0xFFFFC878, L("City of Dispatches", "Город депеш"), L("news, podcasts", "новости, подкасты"),
        L("Towers between which paper dispatch birds fly off every morning.", "Город башен, между которыми по утрам разлетаются бумажные птицы-депеши."),
        Shape.FEATHER,
        listOf(
            Resident(L("The postmaster", "Почтмейстер"), 2, L("Sorts the birds by the colour of their wax.", "Сортирует птиц по цвету сургуча.")),
            Resident(L("The pigeon keeper", "Голубятник"), 4, L("Knows every bird by name and lets the slowest go first.", "Знает каждую птицу по имени и первой выпускает самую медленную.")),
        ),
    ),
    MUSIC(
        0xFFAF91FF, L("Vinyl Sea", "Виниловое море"), L("music", "музыка"),
        L("A night sea whose waves are the grooves of a record. Under the moon you can hear it play.", "Ночное море, волны которого – борозды пластинки. Под луной слышно, как оно играет."),
        Shape.DISC,
        listOf(
            Resident(L("The lighthouse keeper", "Смотритель маяка"), 2, L("His lighthouse turns at 33 revolutions a minute.", "Его маяк вращается со скоростью 33 оборота в минуту.")),
            Resident(L("The record diver", "Ныряльщик за пластинками"), 4, L("Brings up records nobody has heard for a hundred years.", "Достаёт со дна пластинки, которых никто не слышал лет сто.")),
        ),
    ),
    SPORT(
        0xFFFF9682, L("Mountain Stair", "Горная лестница"), L("exercise, yoga", "зарядка, йога"),
        L("A thousand steps to the summit. Someone runs up them every morning: that's you.", "Тысяча ступеней к вершине. Кто-то каждое утро бежит наверх – это вы."),
        Shape.STONE,
        listOf(
            Resident(L("The mountain goat", "Горный козёл"), 2, L("Waits on the hundredth step and pretends he isn't racing you.", "Ждёт на сотой ступени и делает вид, что не соревнуется.")),
            Resident(L("The sherpa", "Шерпа"), 4, L("Carries everyone's tea to the top.", "Носит наверх чай для всех.")),
        ),
    ),
    RADIO(
        0xFF78E6DC, L("Antenna Isles", "Острова антенн"), L("radio, TV", "радио, ТВ"),
        L("An archipelago of masts in the middle of the sea. The air hums over the water.", "Архипелаг мачт посреди моря. Над водой гудит эфир."),
        Shape.BULB,
        listOf(
            Resident(L("The radio operator", "Радистка"), 2, L("Answers every call sign, even the ones from the stars.", "Отвечает на каждый позывной, даже со звёзд.")),
            Resident(L("The Morse hermit", "Морзист-отшельник"), 4, L("Has said nothing aloud for years, only taps.", "Много лет не говорит вслух, только стучит.")),
        ),
    ),
    CINEMA(
        0xFFBED2FF, L("Night Cinema", "Ночной кинотеатр"), L("films, series, video", "кино, сериалы, видео"),
        L("A screen under the open sky. The show never ends and nobody leaves.", "Экран под открытым небом. Сеанс не кончается, зрители не расходятся."),
        Shape.TICKET,
        listOf(
            Resident(L("The projectionist", "Киномеханик"), 2, L("Changes the reels without ever looking.", "Меняет бобины не глядя.")),
            Resident(L("The usher", "Билетёрша"), 4, L("Keeps your seat in the third row.", "Держит для вас место в третьем ряду.")),
        ),
    ),
    GARDEN(
        0xFFBEF0BE, L("Misty Garden", "Туманный сад"), L("slow mornings, audiobooks", "неспешное утро, аудиокниги"),
        L("A garden where the mist lifts slowly and there's nowhere to hurry.", "Сад, где туман уходит медленно и спешить некуда."),
        Shape.LEAF,
        listOf(
            Resident(L("The gardener", "Садовница"), 2, L("Waters the mist so it lasts till noon.", "Поливает туман, чтобы он продержался до полудня.")),
            Resident(L("The hedgehog watchman", "Ёж-сторож"), 4, L("Guards the path and snores.", "Сторожит тропинку и сопит.")),
        ),
    ),
    WILD(
        0xFFDC8CFF, L("Wild World", "Дикий мир"), L("a surprise from your «someday» list", "сюрприз из списка «когда-нибудь»"),
        L("Sometimes the portal opens by itself onto a world nobody has seen. Inside is something from your «someday» list.", "Иногда портал сам открывается в мир, которого никто не видел. Внутри – что-то из вашего списка «когда-нибудь»."),
        Shape.KEY, emptyList(),
    );

    companion object {
        /** The worlds a day can be given; the wild one comes only by itself. */
        val chosen = entries.filter { it != WILD }
    }
}

data class Find(val key: String, val world: World, val name: L, val rare: Boolean, val shape: Shape = world.shape)

object Finds {
    private fun f(key: String, w: World, en: String, ru: String, rare: Boolean = false, shape: Shape = w.shape) = Find(key, w, L(en, ru), rare, shape)

    val all = listOf(
        f("news_feather", World.NEWS, "Dispatch feather", "Перо депеши"),
        f("news_seal", World.NEWS, "Wax seal", "Сургучная печать", shape = Shape.CIRCLE),
        f("news_extra", World.NEWS, "Extra edition", "Экстренный выпуск", shape = Shape.TICKET),
        f("news_stamp", World.NEWS, "Postmark", "Почтовый штемпель", shape = Shape.TICKET),
        f("news_quill", World.NEWS, "Golden quill", "Золотое перо", true),
        f("news_ring", World.NEWS, "Pigeon's ring", "Голубиное колечко", true, shape = Shape.CIRCLE),

        f("music_shard", World.MUSIC, "Vinyl shard", "Осколок пластинки"),
        f("music_shell", World.MUSIC, "Echo shell", "Ракушка с эхом", shape = Shape.SHELL),
        f("music_bottle", World.MUSIC, "Note in a bottle", "Нота в бутылке", shape = Shape.BOTTLE),
        f("music_pick", World.MUSIC, "Pearl pick", "Перламутровый медиатор", shape = Shape.DROP),
        f("music_needle", World.MUSIC, "Silver needle", "Серебряная игла", true, shape = Shape.CRYSTAL),
        f("music_label", World.MUSIC, "First-press label", "Наклейка первого тиража", true, shape = Shape.CIRCLE),

        f("sport_stone", World.SPORT, "Stair stone", "Камень со ступени"),
        f("sport_flag", World.SPORT, "Summit flag", "Флажок с вершины", shape = Shape.FLAG),
        f("sport_crystal", World.SPORT, "Ice crystal", "Ледяной кристалл", shape = Shape.CRYSTAL),
        f("sport_knot", World.SPORT, "Rope knot", "Узел верёвки", shape = Shape.CIRCLE),
        f("sport_eagle", World.SPORT, "Eagle feather", "Орлиное перо", true, shape = Shape.FEATHER),
        f("sport_edelweiss", World.SPORT, "Edelweiss", "Эдельвейс", true, shape = Shape.STAR),

        f("radio_valve", World.RADIO, "Transmitter valve", "Лампа передатчика"),
        f("radio_morse", World.RADIO, "Morse tape", "Лента с морзянкой", shape = Shape.TICKET),
        f("radio_qsl", World.RADIO, "QSL card", "Карточка-позывной", shape = Shape.TICKET),
        f("radio_coil", World.RADIO, "Copper coil", "Медная катушка", shape = Shape.CIRCLE),
        f("radio_quartz", World.RADIO, "Quartz crystal", "Кварц", true, shape = Shape.CRYSTAL),
        f("radio_buoy", World.RADIO, "Buoy lamp", "Огонь с буя", true),

        f("cinema_ticket", World.CINEMA, "Ticket stub", "Корешок билета"),
        f("cinema_frame", World.CINEMA, "Film frame", "Кадр плёнки", shape = Shape.TICKET),
        f("cinema_popcorn", World.CINEMA, "Popcorn from 1959", "Попкорн 1959 года", shape = Shape.SHELL),
        f("cinema_glasses", World.CINEMA, "3D glasses", "Стереоочки"),
        f("cinema_reel", World.CINEMA, "Golden reel", "Золотая бобина", true, shape = Shape.CIRCLE),
        f("cinema_star", World.CINEMA, "Poster star", "Звезда с афиши", true, shape = Shape.STAR),

        f("garden_leaf", World.GARDEN, "Dry leaf", "Сухой лист"),
        f("garden_dew", World.GARDEN, "Dew drop", "Капля росы", shape = Shape.DROP),
        f("garden_seed", World.GARDEN, "Mist seed", "Семечко тумана", shape = Shape.DROP),
        f("garden_snail", World.GARDEN, "Snail shell", "Ракушка улитки", shape = Shape.SHELL),
        f("garden_clock", World.GARDEN, "Clock flower", "Цветок-часы", true, shape = Shape.STAR),
        f("garden_moth", World.GARDEN, "Moon moth", "Лунная бабочка", true, shape = Shape.FEATHER),

        f("wild_key", World.WILD, "Key to no door", "Ключ без двери"),
        f("wild_map", World.WILD, "Scrap of a strange map", "Обрывок чужой карты", shape = Shape.TICKET),
        f("wild_compass", World.WILD, "Lying compass", "Компас, который врёт", true, shape = Shape.CIRCLE),
        f("wild_star", World.WILD, "Fallen star", "Упавшая звезда", true, shape = Shape.STAR),
    )

    val byKey = all.associateBy { it.key }
    fun of(w: World) = all.filter { it.world == w }
}

/** The arch's frame, rebuilt as finds pile up. */
data class Frame(val key: String, val name: L, val a: Long, val b: Long, val joint: Long, val need: Int, val leaves: Boolean = false)

object Frames {
    val all = listOf(
        Frame("wood", L("Wood", "Дерево"), 0xFF5A3D25, 0xFF7A5534, 0xFF3A2716, 0),
        Frame("stone", L("Stone", "Камень"), 0xFF4C4957, 0xFF6A6676, 0xFF34313D, 0),
        Frame("brass", L("Brass", "Латунь"), 0xFF8A6A2C, 0xFFC9A24F, 0xFF5E4718, 10),
        Frame("ice", L("Ice", "Лёд"), 0xFF7FB8CC, 0xFFCDEEF7, 0xFF5D93A6, 20),
        Frame("vine", L("Vine", "Лоза"), 0xFF2F4F2B, 0xFF4F7A45, 0xFF1F361C, 30, leaves = true),
    )
    fun of(key: String) = all.firstOrNull { it.key == key } ?: all[1]
}
