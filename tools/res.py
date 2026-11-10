"""Writes the string resources (English and Russian) from one table, so the two never drift apart.

python tools/res.py
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"

S = [
    ("app_name", "Threshold", "Порог"),
    ("settings", "Settings", "Настройки"),
    ("close", "Close", "Закрыть"),
    ("cancel", "Cancel", "Отмена"),
    ("ok", "OK", "Готово"),
    ("none", "None", "Нет"),
    ("off", "Off", "Выкл."),
    ("today", "today", "сегодня"),
    ("tomorrow", "tomorrow", "завтра"),
    ("min_n", "%1$s min", "%1$s мин"),
    ("sec_n", "%1$d s", "%1$d с"),
    ("tab_home", "Arch", "Арка"),
    ("tab_week", "Week", "Неделя"),
    ("tab_worlds", "Worlds", "Миры"),
    ("tab_finds", "Finds", "Находки"),
    ("k_app", "App", "Приложение"),
    ("k_link", "Link", "Ссылка"),
    ("k_radio", "Radio", "Радио"),
    ("k_file", "File", "Файл"),

    # notifications
    ("ch_ring", "The portal (alarm)", "Портал (будильник)"),
    ("ch_day", "Evening and morning", "Вечер и утро"),
    ("ch_event", "Events of the day", "События дня"),
    ("n_event_b", "Tap to open", "Нажмите, чтобы открыть"),
    ("n_event_radio", "The radio is playing", "Радио уже играет"),
    ("a_open", "Open", "Открыть"),
    ("n_ring", "The portal is opening · %1$s", "Открывается портал · %1$s"),
    ("a_snooze", "%1$d more minutes", "Ещё %1$d минут"),
    ("n_radio", "The radio is on", "Играет радио"),
    ("a_radio_stop", "Turn off", "Выключить"),
    ("n_snoozed", "The portal will open again at %1$s", "Портал откроется снова в %1$s"),
    ("n_snoozed_b", "It's trembling, but holding.", "Он дрожит, но держится."),
    ("a_up_now", "Get up now", "Встать сейчас"),
    ("n_evening", "Time to close the day", "Пора закрывать день"),
    ("n_evening_b", "Close it now and the portal will open wide in the morning.", "Закройте день сейчас – и утром выпадут две находки."),
    ("a_close_day", "Close the day", "Закрыть день"),
    ("n_next", "The portal leads on", "Портал ведёт дальше"),
    ("n_nudge", "Time to go", "Пора"),
    ("n_nudge_b", "The morning is over. The world will still be there tomorrow.", "Утро закончилось. Мир никуда не денется до завтра."),

    # home
    ("home_title", "The keeper's room", "Комната привратника"),
    ("home_unready", "The alarm may not ring: the phone hasn't allowed everything yet", "Будильник может не прозвенеть: телефон разрешил ещё не всё"),
    ("home_snoozed", "The portal opens again at %1$s", "Портал откроется снова в %1$s"),
    ("home_snoozed_b", "It narrowed to a slit when you asked for more time.", "Вы отложили будильник, и портал почти закрылся."),
    ("home_none", "No alarm set", "Будильник не стоит"),
    ("home_none_b", "Every day is off. Open a day on the Week tab.", "Все дни выходные. Включите день во вкладке «Неделя»."),
    ("home_next", "Next morning · %1$s", "Следующее утро · %1$s"),
    ("home_wild", "The wild portal: a surprise from your «someday» list", "Дикий портал: сюрприз из списка «когда-нибудь»"),
    ("home_plain", "Just the alarm, nothing opens", "Просто будильник, ничего не открывается"),
    ("home_charged_full", "Day closed at %1$s. The portal is %2$d%% charged and will open wide: two finds will fall out.", "День закрыт в %1$s. Встаньте с первого звонка – и выпадут две находки."),
    ("home_charged", "Day closed at %1$s. The portal is %2$d%% charged: one find will fall out.", "День закрыт в %1$s. Утром выпадет одна находка."),
    ("home_far", "Close the day in the evening before: the earlier, the wider the portal opens.", "Закройте день накануне вечером: чем раньше, тем больше находок утром."),
    ("home_uncharged", "The portal isn't charged: the day isn't closed yet.", "День ещё не закрыт. Закройте его вечером – утром будет больше находок."),
    ("home_close", "Hold to close the day", "Удерживайте, чтобы закрыть день"),
    ("home_reopen", "Day closed · open it again", "День закрыт · открыть снова"),
    ("home_reopen_at", "Day closed at %1$s · open it again", "День закрыт в %1$s · открыть снова"),
    ("home_skip", "Don't wake me %1$s", "Не будить %1$s"),
    ("home_unskip", "Wake me %1$s after all", "Всё же будить %1$s"),
    ("home_streak", "Mornings without «five more minutes»: %1$d in a row", "Утра без «ещё пять минут»: %1$d подряд"),
    ("home_later", "Later today", "Сегодня ещё"),
    ("toast_skipped", "Won't wake you %1$s", "Не разбужу %1$s"),

    # week
    ("week_title", "Week", "Неделя"),
    ("week_sub", "Each day opens onto its own world", "Каждый день открывается в свой мир"),
    ("week_off", "Day off", "Выходной"),
    ("week_off_b", "The portal is bricked up. Sleep.", "Портал заложен камнями. Спите."),
    ("week_plain", "Just the alarm", "Просто будильник"),
    ("week_skip_once", "Not this time (%1$s)", "В этот раз не будить (%1$s)"),
    ("week_holiday", "Holiday (%1$s): no alarm", "Праздник (%1$s): не будить"),
    ("week_vacation", "On vacation: no alarm", "Отпуск: не будить"),
    ("week_wild", "The wild portal opens this time", "В этот раз откроется дикий портал"),
    ("week_rules", "Rules", "Правила"),
    ("rule_holidays", "No alarm on public holidays", "Не будить в праздники"),
    ("rule_holidays_b", "The main dates of the Russian calendar; add others as days off", "Основные даты российского календаря; остальные отметьте сами"),
    ("rule_vacation", "Vacation", "Отпуск"),
    ("rule_vacation_none", "not set", "не задан"),
    ("rule_vacation_clear", "Clear the vacation", "Убрать отпуск"),
    ("rule_wild", "Wild portal once a week", "Дикий портал раз в неделю"),
    ("rule_wild_b", "One morning a week brings something from the «someday» list instead", "Одно утро в неделю вместо обычного откроет что-то из списка «когда-нибудь»"),
    ("rule_wishes", "The «someday» list", "Список «когда-нибудь»"),
    ("toast_weekdays", "All weekdays now at %1$s", "Теперь все будни в %1$s"),

    # day
    ("day_on", "Wake me on this day", "Будить в этот день"),
    ("day_time", "The portal opens at", "Портал открывается в"),
    ("day_time_change", "Change", "Изменить"),
    ("day_time_all", "Same time on every weekday", "Такое же время во все будни"),
    ("day_auto", "Open at once, no touching", "Открывать сразу, без касаний"),
    ("day_auto_b", "When the alarm rings, the app opens by itself; the phone only asks to be unlocked. No circle, the finds are the same.", "Когда зазвонит, приложение откроется само – телефон лишь попросит разблокировку. Без круга, находки те же."),
    ("week_auto", "opens by itself", "открывается само"),
    ("day_steps", "What comes through", "Что откроется"),
    ("day_steps_none", "Nothing yet: the alarm will just ring. Add an app, a link, a radio station or a file from the phone.", "Пока ничего: будильник просто прозвенит. Добавьте приложение, ссылку, радио или файл с телефона."),
    ("day_then", "after %1$d min the portal leads on", "через %1$d мин портал поведёт дальше"),
    ("day_add", "Add:", "Добавить:"),
    ("day_lasts", "Lasts, min", "Длится, мин"),
    ("day_share_hint", "Tip: in YouTube or Yandex Music tap «Share» → Threshold to put a playlist here.", "Подсказка: в YouTube или Яндекс Музыке нажмите «Поделиться» → «Порог», и плейлист окажется здесь."),
    ("day_try", "Try opening it now", "Проверить: открыть сейчас"),
    ("day_world", "World", "Мир"),
    ("day_world_b", "Where the portal opens: chosen by what comes through, change it if you like.", "Куда открывается портал. Выбирается сам по тому, что откроется; можно поменять."),
    ("toast_cant_open", "Couldn't open it: is the app installed?", "Не получилось открыть: приложение установлено?"),

    # pickers
    ("pick_app", "Choose an app", "Выберите приложение"),
    ("pick_search", "Search", "Поиск"),
    ("pick_loading", "Looking through the apps…", "Смотрю, что установлено…"),
    ("pick_link", "A link", "Ссылка"),
    ("pick_link_b", "A particular video, film, playlist or album. Easiest: in the app tap «Share» → Threshold. It opens right there, in its own app.", "Конкретное видео, фильм, плейлист или альбом. Проще всего нажать в самом приложении «Поделиться» → «Порог». Откроется сразу там, в своём приложении."),
    ("pick_url", "Address", "Адрес"),
    ("pick_paste", "Paste from clipboard", "Вставить из буфера"),
    ("pick_label", "Name", "Название"),
    ("pick_opens_in", "Opens in %1$s", "Откроется в «%1$s»"),
    ("pick_not_taken", "%1$s doesn't open links like this one, so the page will open in the browser. For Kinopoisk use a kinopoisk.ru/film/… link.", "%1$s не открывает такие ссылки, страница откроется в браузере. Для Кинопоиска нужна ссылка вида kinopoisk.ru/film/…"),
    ("pick_opens_browser", "Opens in the app Android picks for it", "Откроется в приложении, которое выберет Android"),
    ("pick_save", "Save", "Сохранить"),
    ("pick_radio", "Radio", "Радио"),
    ("pick_radio_b", "The portal plays the radio itself, and the station becomes the alarm sound: the volume rises from zero.", "Радио играет сам портал, и эфир становится звонком: громкость нарастает с нуля."),
    ("pick_radio_own", "Your own stream", "Свой поток"),
    ("pick_stream", "Stream address (mp3, aac)", "Адрес потока (mp3, aac)"),
    ("wishes_title", "Someday", "Когда-нибудь"),
    ("wishes_b", "Albums, films, lectures you keep meaning to get to. Once a week the wild portal brings one of them in the morning.", "Альбомы, фильмы, лекции, до которых всё не доходят руки. Раз в неделю дикий портал принесёт что-то одно утром."),
    ("wishes_none", "The list is empty for now.", "Пока список пуст."),
    ("wishes_off", "The wild portal is off: turn it on in the Week rules.", "Дикий портал выключен: включите его в правилах недели."),

    # share
    ("share_title", "Where shall I put it?", "Куда положить?"),
    ("share_none", "There's no link in what was shared.", "В том, чем поделились, нет ссылки."),
    ("share_after", "after: %1$s", "после: %1$s"),
    ("share_wish", "To the «someday» list", "В список «когда-нибудь»"),
    ("share_wish_b", "The wild portal will bring it one morning.", "Дикий портал принесёт это однажды утром."),
    ("toast_added", "Added to %1$s", "Добавлено: %1$s"),
    ("toast_wish", "Added to «someday»", "Добавлено в «когда-нибудь»"),

    # worlds
    ("worlds_title", "Worlds", "Миры"),
    ("worlds_sub", "The more you go, the more you see", "Чем чаще вы туда ходите, тем больше видно"),
    ("worlds_you", "your arch", "ваша арка"),
    ("worlds_level", "level %1$d · %2$d mornings", "ур. %1$d · утр: %2$d"),
    ("worlds_wild_sub", "opens by itself", "открывается сам"),
    ("worlds_next", "Mornings here: %1$d. Next level in %2$d.", "Утр здесь: %1$d. До следующего уровня: %2$d."),
    ("worlds_top", "Mornings here: %1$d. The world is fully open.", "Утр здесь: %1$d. Мир открыт целиком."),
    ("worlds_residents", "Residents", "Жители"),
    ("worlds_resident_locked", "Someone else lives here: you'll meet at level %1$d", "Здесь живёт кто-то ещё: познакомитесь на %1$d-м уровне"),
    ("journal", "Morning journal", "Журнал утр"),
    ("journal_empty", "No mornings yet. The first one will appear here.", "Утр пока не было. Первое появится здесь."),
    ("journal_wakes", "mornings", "утр"),
    ("journal_first", "at the first ring", "с первого звонка"),
    ("journal_step", "to step through", "до шага"),
    ("journal_mood", "How the worlds wake you", "Как будят миры"),
    ("journal_best", "You wake up freshest in %1$s.", "Бодрее всего вы просыпаетесь в мире «%1$s»."),
    ("journal_worst", "Mornings in %1$s go hardest. Maybe give that day another world?", "Тяжелее всего утра в мире «%1$s». Может, дать этому дню другой мир?"),
    ("journal_rate", "Say how the morning went after stepping through, and here you'll see which world wakes you best.", "Отмечайте после шага, каким было утро, и здесь станет видно, какой мир будит лучше."),
    ("mood_fresh", "Fresh", "Бодро"),
    ("mood_so", "So-so", "Так себе"),
    ("mood_hard", "Hard", "Тяжело"),

    # finds
    ("finds_title", "Finds", "Находки"),
    ("finds_sub", "Found %1$d of %2$d", "Собрано %1$d из %2$d"),
    ("finds_rare", "rare", "редкая"),
    ("finds_frame", "The arch's frame", "Рама арки"),
    ("finds_frame_b", "Rebuilt as the finds pile up", "Перестраивается по мере того, как копятся находки"),
    ("finds_need", "%1$d finds", "нужно %1$d"),

    # alarm screen
    ("al_hint_open", "The portal is opening… Trace a circle with your finger to hold it.", "Портал открывается… Ведите пальцем по кругу, чтобы удержать его."),
    ("al_hint_more", "More… the portal is getting stronger", "Ещё… портал крепнет"),
    ("al_hint_half", "Almost. Half a circle more", "Почти. Ещё полкруга"),
    ("al_hint_ready", "The portal holds. Step through.", "Портал стоит. Шагайте."),
    ("al_step", "Step through", "Шагнуть"),
    ("al_no_snooze", "No more snoozing today", "Больше откладывать нельзя"),
    ("al_test_ring", "A test ring: no snoozing", "Это проверка: откладывать нельзя"),
    ("al_hint_auto", "The portal is opening by itself…", "Портал открывается сам…"),
    ("al_hint_auto_unlock", "Unlock the phone and the morning comes in.", "Разблокируйте телефон, и утро начнётся само."),
    ("al_auto_found", "Fell out of the world: %1$s", "Из мира выпало: %1$s"),
    ("al_gone", "The portal has closed", "Портал закрылся"),
    ("al_through", "You stepped into", "Вы шагнули в"),
    ("al_plain", "Good morning.", "Доброе утро."),
    ("al_radio", "Now playing: %1$s", "Играет %1$s"),
    ("al_opens", "Opening: %1$s", "Открывается: %1$s"),
    ("al_chain", "In %1$d min the portal leads on: %2$s", "Через %1$d мин портал поведёт дальше: %2$s"),
    ("al_test", "That was a test: nothing went into the journal.", "Это была проверка: в журнал ничего не записано."),
    ("al_no_finds", "The portal had narrowed: nothing fell out this time.", "Портал почти закрылся – в этот раз ничего не выпало."),
    ("al_finds_one", "Something fell out of the world", "Из мира выпала находка"),
    ("al_finds_two", "The portal was wide: two finds fell out", "Вы встали вовремя – выпали две находки"),
    ("al_how", "How was waking up?", "Как проснулись?"),
    ("al_open", "Open", "Открыть"),
    ("al_done", "Good", "Хорошо"),

    # settings
    ("set_alarm", "Alarm", "Будильник"),
    ("set_snooze_len", "Snooze for", "Откладывать на"),
    ("set_snooze_max", "Snoozes allowed", "Сколько раз можно отложить"),
    ("set_snooze_note", "After one snooze, one find falls out; after two, none.", "После одного «ещё» выпадает одна находка, после двух – ни одной."),
    ("set_ramp", "Volume rises over", "Громкость нарастает за"),
    ("set_vibrate", "Vibrate", "Вибрация"),
    ("set_test", "Test the alarm now", "Проверить будильник сейчас"),
    ("set_test_b", "The portal rings at once, as in the morning; nothing goes into the journal.", "Портал зазвонит сразу, как утром; в журнал ничего не попадёт."),
    ("set_day", "Evening and morning", "Вечер и утро"),
    ("set_evening", "Remind me to close the day", "Напоминать закрыть день"),
    ("set_evening_b", "Eight and a half hours before the alarm", "За восемь с половиной часов до будильника"),
    ("set_nudge", "«Time to go», minutes after stepping through", "«Пора» через сколько минут после шага"),
    ("set_nudge_b", "So the morning's video doesn't turn into the whole morning", "Чтобы утреннее видео не съело всё утро"),
    ("set_more", "More", "Ещё"),
    ("set_lang", "Language", "Язык"),
    ("lang_system", "System", "Как в системе"),
    ("rel_title", "Will it ring?", "Прозвенит ли?"),
    ("rel_sub", "What the phone has to allow", "Что должен разрешить телефон"),
    ("rel_intro", "An alarm has to wake a sleeping phone and show over the lock screen. Check each line below; green means it's fine.", "Будильник должен разбудить спящий телефон и показаться поверх экрана блокировки. Проверьте каждую строку: галочка значит, что всё в порядке."),
    ("rel_notif", "Notifications", "Уведомления"),
    ("rel_notif_b", "The ringing portal is a notification", "Звонящий портал – это уведомление"),
    ("rel_full", "Full-screen alarm", "Будильник на весь экран"),
    ("rel_full_b", "Shows the portal over the lock screen", "Показывает портал поверх экрана блокировки"),
    ("rel_exact", "Alarms on time", "Будильники точно в срок"),
    ("rel_exact_b", "Without it Android may ring minutes late", "Без этого Android может позвонить на несколько минут позже"),
    ("rel_battery", "No battery saving for the app", "Без экономии батареи"),
    ("rel_battery_b", "So the phone doesn't put the portal to sleep at night", "Чтобы телефон не усыпил портал ночью"),
    ("rel_fix", "Allow", "Разрешить"),
    ("rel_xiaomi", "Xiaomi, Redmi, POCO", "Xiaomi, Redmi, POCO"),
    ("rel_xiaomi_b", "These phones close apps at night and wipe their alarms. Turn on «Autostart» and, under «Other permissions», «Show on lock screen» and «Display pop-up windows while running in the background». On the first morning the phone asks «Allow Threshold to open …?»: choose «Always allow».", "Эти телефоны ночью закрывают приложения и стирают их будильники. Включите «Автозапуск», а в «Других разрешениях» – «Экран блокировки» и «Всплывающие окна в фоне». В первое утро телефон спросит «Разрешить Порогу открыть…?» – выберите «Всегда разрешать»."),
    ("rel_autostart", "Autostart", "Автозапуск"),
    ("rel_other", "Other permissions", "Другие разрешения"),
    ("ev_title", "Through the day", "В течение дня"),
    ("ev_b", "Other things this day holds: an evening book, a workout, a film. At the time a notification comes; tap it and the app, link or file opens, while a radio starts by itself. They come even on days the morning alarm is off.", "Другие дела этого дня: книга вечером, тренировка, фильм. В нужное время придёт уведомление: нажмёте – и откроется приложение, ссылка или файл, а радио заиграет само. Приходят, даже если утром будильник выключен."),
    ("ev_add", "Event", "Событие"),
    ("ev_new", "New event", "Новое событие"),
    ("ev_one", "Event", "Событие"),
    ("ev_time", "At", "Время"),
    ("ev_what", "What opens", "Что откроется"),
    ("ev_what_none", "Choose an app, a link, a radio station or a file:", "Выберите приложение, ссылку, радио или файл:"),
    ("ev_change", "Replace with:", "Заменить на:"),
    ("ev_days", "Days", "По каким дням"),
    ("ev_every_day", "Every day", "Каждый день"),
    ("ev_weekdays", "Weekdays", "Будни"),
    ("ev_weekend", "Weekend", "Выходные"),
    ("ev_on", "On", "Включено"),
    ("ev_paused", "paused", "на паузе"),
    ("ev_how", "Android doesn't let an app open another one by itself while you use the phone, so an event comes as a notification with a sound. A radio starts playing at once.", "Android не даёт приложению самому открывать другие, пока вы пользуетесь телефоном, поэтому событие приходит уведомлением со звуком. Радио включается сразу."),
    ("ev_saved", "Saved: %1$s", "Сохранено: %1$s"),
    ("ev_delete", "Delete event", "Удалить событие"),

    ("widget_name", "Next morning", "Следующее утро"),
    ("widget_desc", "When the alarm rings, and the day's events", "Когда зазвонит будильник и что ещё сегодня"),
    ("w_next", "Alarm %1$s", "Будильник %1$s"),
    ("w_snoozed", "Snoozed until", "Отложен до"),
    ("w_wild", "The wild portal: a surprise", "Дикий портал: сюрприз"),
    ("w_none", "No alarm set", "Будильник не стоит"),

    ("about_version", "Threshold %1$s", "Порог %1$s"),

    ("community_text", "Threshold is one of the StillCraft apps. Tell us what's broken or what's missing.", "«Порог» – одно из приложений StillCraft. Расскажите, что сломалось или чего не хватает."),
    ("community_bug", "Report a bug", "Сообщить об ошибке"),
    ("community_bug_sub", "opens the bot in Telegram", "откроется бот в Телеграме"),
    ("community_idea", "Suggest an idea", "Предложить идею"),
    ("community_idea_sub", "a world, a feature, anything", "мир, функция, что угодно"),
    ("community_follow", "Follow updates", "Следить за обновлениями"),
    ("community_follow_sub", "a message when a new version is out", "сообщение, когда выйдет новая версия"),
    ("community_chat", "StillCraft chat", "Чат StillCraft"),
    ("community_support", "Support Threshold", "Поддержать «Порог»"),
    ("community_support_sub", "for new worlds behind the portal", "на новые миры за порталом"),
]


def esc(s: str) -> str:
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = s.replace("'", "\\'").replace('"', '\\"')
    if s.startswith("@") or s.startswith("?"):
        s = "\\" + s
    return s


def write(folder: str, col: int):
    keys = [k for k, *_ in S]
    dup = {k for k in keys if keys.count(k) > 1}
    assert not dup, dup
    out = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for row in S:
        attrs = ' translatable="false"' if row[0] == "app_name_x" else ""
        out.append(f'    <string name="{row[0]}"{attrs}>{esc(row[col])}</string>')
    out.append("</resources>\n")
    d = RES / folder
    d.mkdir(parents=True, exist_ok=True)
    (d / "strings.xml").write_text("\n".join(out), encoding="utf-8")


write("values", 1)
write("values-ru", 2)
print(len(S), "strings")
