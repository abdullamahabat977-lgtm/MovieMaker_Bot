# د Movie Studio v2 د پروژې د معلوماتو بڼه

## موخه
د فلم ټول معلومات په یوه نسخه لرونکي JSON فایل کې ساتل کېږي. فایل او اړوند assets یوازې د موبایل په داخلي ذخیره کې پاتې کېږي.

## د فولډر جوړښت
MovieProjects/project_<id>/project.json
MovieProjects/project_<id>/assets/characters/
MovieProjects/project_<id>/assets/environments/
MovieProjects/project_<id>/assets/props/
MovieProjects/project_<id>/assets/images/
MovieProjects/project_<id>/assets/audio/
MovieProjects/project_<id>/exports/

## د پروژې د JSON لومړنی جوړښت
{
  "schemaVersion": 1,
  "project": {
    "id": "project_001",
    "name": "زما لومړی فلم",
    "createdAt": "ISO-8601",
    "updatedAt": "ISO-8601",
    "settings": { "width": null, "height": null, "fps": null, "renderMode": "3d" }
  },
  "assets": [],
  "scenes": [],
  "timeline": { "sceneOrder": [] }
}

## د اصولو لنډیز
1. هر Project، Asset، Scene، Object، Track او Clip ځانګړی ID لري.
2. د مهال‌وېش وخت په فریمونو ساتل کېږي.
3. د فایل لارې د پروژې دننه نسبي لارې وي؛ مطلق Android paths یا web URLs مه ساتئ.
4. د assets فایلونه د موبایل په پروژه فولډر کې وي.
5. د اصلي اپ 3D ماډلونه د APK دننه ساتل کېږي؛ د کاروونکي خپل فایلونه په موبایل کې پاتې کېږي.
6. د فلمونو او صادر شوو ویډیوګانو Cloud upload نشته.
7. د خوندي کولو پر مهال د موقتي فایل او backup په وسیله د پخوانیو معلوماتو د له منځه تلو مخه ونیسئ.
8. د schemaVersion بدلون باید د پخوانیو پروژو د migration لاره ولري.

## د تطبیق حالت
دا سند د معلوماتو قرارداد دی. د بشپړ Scene editor، rig، animation او exporter کوډ لا په راتلونکو پړاوونو کې جوړېږي. یوازې د دې سند له شتون څخه د بشپړ فلم جوړولو وړتیا مه اټکل کوئ.
