# 3D Workspace and Android Lifecycle

## اوسنی جوړښت

- `MainActivity` د کور پاڼه، د پروژو لېست، محلي asset catalogue او د حساب لینک اداره کوي.
- `SceneEditorActivity` د پروژې لومړۍ صحنه، د GLB توکو لېست، محلي ذخیره، موقعیت، اندازه، څرخول او Timeline metadata اداره کوي.
- `RealisticModelPreviewActivity` د یوه GLB ماډل mesh، PBR مواد، textures او embedded skeletal animations د Filament له لارې ښيي.
- `MultiModelSceneActivity` د پروژې څو وارد شوي GLB ماډلونه په یوه Filament scene کې ښيي؛ اوسنی حد ۸ ماډلونه او ۸۰ MB دی.
- د کاروونکي پروژې، ماډلونه او د فلم فایلونه د موبایل په داخلي ذخیره کې ساتل کېږي؛ د فلم فایلونه کلاوډ ته نه ځي.
- د asset catalogue اوس د توکو نومونه او توضیحات لري، خو د انسانانو، حیواناتو، موټرو او ودانیو حقیقي GLB فایلونه لا د اپ دننه نه دي برابر شوي.
- د صحنې توکي په GLB 2.0 بڼه واردېږي؛ د هر فایل حد ۴۰ MB، د پروژې ټول GLB فایلونه ۲۵۶ MB، او په یوه صحنه کې ۳۲ توکي دي.
- د Scene Editor له لارې د X/Y/Z موقعیت، اندازه، د X/Y/Z محورونو څرخول او دقیق عددي بدلونونه خوندي کېږي.
- د Timeline کلیپونه د action، object، start frame او duration frame metadata ثبتوي. د کلیپونو playback لا د GLB skeletal animations سره نه دی تړل شوی.
- د GLB preview کې embedded animations ټاکل، درول او بیا چلول شوني دي.
- د ګډ scene renderer کې د توکو touch picking او د انتخاب شوي ماډل live transform لا نه دي بشپړ شوي.
- د کمرې او رڼا بشپړ editor، غږ/ډیالوګ tracks، multi-scene timeline، frame renderer او MP4 export لا نه دي جوړ شوي.

## د APK د جوړولو تګلاره

د پراختیا پر مهال د اتومات push-triggered APK build مخه نیول شوې ده. د GitHub Actions workflow یوازې د لاس په workflow_dispatch چلول کېږي. تر څو د اصلي فلم جوړولو برخې د ازموینې وړ نه وي، نوې APK مه جوړوئ.

## Lifecycle او د حافظې اصول

1. د Filament viewer او SurfaceView جوړول د Activity د ژوند دورې دننه وساتئ.
2. د Activity د pause پر مهال frame callback بند او د GLB resource loader سرچینې خوشې کړئ.
3. د بیا resume پر مهال viewer او ماډلونه په کنټرول شوې توګه بېرته چمتو کړئ.
4. د ماډلونو د حافظې حدونه مراعات کړئ؛ د ناکام ماډل له امله نور سالم ماډلونه مه بندوئ.
5. د پروژې فایلونه د خوندي temporary/backup لیکلو له لارې تازه کړئ.

## د وسیلې د ازموینې چک لېست

- د Android ES 3.0 ملاتړ او د Filament initialization تاییدول.
- یو GLB واردول، جلا preview کول او embedded animation بدلول/درول/چلول.
- څو GLB ماډلونه په ګډه صحنه کې ښودل او د حدونو د پوره کېدو چلند کتل.
- د موقعیت، اندازې او درې واړو rotation محورونو بدلون، د اپ بیا پرانیستل او د معلوماتو دوام تاییدول.
- د Timeline کلیپ د مختلف duration frame سره ثبت او حذفول.
- Activity ته څو ځله داخلېدل، system Back، background/foreground او memory pressure ازمویل.
- د ټیټ حافظې Android وسیلې پر حقیقي hardware ازموینه؛ د CI compile د device test بدیل نه دی.

## د ازموینې او ادعا اصول

تر هغې چې APK په حقیقي وسیله نصب او وازمویل نه شي، د موبایل د نصب، ثبات یا د رینډر د کیفیت بریالیتوب مه ادعا کوئ. د GLB د حقیقي ظاهري کیفیت مسؤلیت د وارد شوي ماډل په geometry، texture، material او rig کیفیت پورې تړلی دی.
