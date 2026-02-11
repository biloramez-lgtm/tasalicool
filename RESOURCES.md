# 📸 ملف الصور والموارد

## صور أوراق اللعب المستخدمة

جميع صور أوراق اللعب موضحة في المشروع. يتم إنشاؤها ديناميكياً من كود Compose بدون الحاجة لصور فعلية.

## هيكل الموارد

```
app/src/main/res/
├── drawable/
│   └── [صور الأوراق ستُنشأ ديناميكياً]
├── values/
│   ├── strings.xml (النصوص)
│   ├── colors.xml (الألوان)
│   └── dimens.xml (الأبعاد)
├── mipmap/
│   └── ic_launcher.png (أيقونة التطبيق)
└── layout/ (للمكونات الثابتة)
```

## الأوراق المدعومة

### الأشكال (Suits):
- ♥ Hearts (قلوب) - أحمر
- ♦ Diamonds (بنوك) - أحمر
- ♣ Clubs (هندي) - أسود
- ♠ Spades (بيك) - أسود

### الرتب (Ranks):
- A (Ace/1)
- 2-9
- 10
- J (Jack)
- Q (Queen)
- K (King)

## الألوان المستخدمة

```kotlin
// الأوراق الحمراء
Red: Color.Red (FF0000)

// الأوراق السوداء
Black: Color.Black (000000)

// خلفية الورقة
White: Color.White (FFFFFF)

// خلفية الدك
Blue: Color(0xFF1565C0)

// الحدود
Gray: Color.Gray (808080)
```

## نصائح لإضافة صور حقيقية

إذا أردت استخدام صور حقيقية للأوراق:

### 1. أضف الصور

```
app/src/main/res/drawable/
├── card_ah.png (Ace of Hearts)
├── card_2h.png
├── card_kd.png (King of Diamonds)
...
```

### 2. عدّل CardView.kt

```kotlin
@Composable
fun CardView(card: Card) {
    Image(
        painter = painterResource(id = getCardResourceId(card)),
        contentDescription = card.toString()
    )
}

fun getCardResourceId(card: Card): Int {
    val name = "${card.rank.displayName.lowercase()}_of_${card.suit.name.lowercase()}"
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}
```

### 3. استخدم مكتبات مجانية

**مصادر الصور المجانية:**
- https://opengameart.org/content/playing-cards-vector-png
- https://www.kenney.nl/assets/playing-cards
- https://www.clipart.email/download/1866656.html

## المتطلبات التقنية للصور

- **الحجم**: 1024 × 1024 بكسل (مثالي)
- **الصيغة**: PNG مع شفافية
- **الحجم الملف**: أقل من 500 KB لكل صورة
- **الدقة**: 72-150 DPI

## التخزين المؤقت

الصور مُخزنة مؤقتاً في الذاكرة لتحسين الأداء:

```kotlin
// Cache للصور
object ImageCache {
    private val cache = mutableMapOf<String, Bitmap>()
    
    fun get(name: String): Bitmap? = cache[name]
    fun put(name: String, bitmap: Bitmap) {
        cache[name] = bitmap
    }
    fun clear() = cache.clear()
}
```

## الأداء والتحسينات

### استهلاك الذاكرة

عند استخدام صور حقيقية:
- حجم الصورة الواحدة: ~100-200 KB
- 52 ورقة: ~5-10 MB
- مع Compose: ~15 MB إجمالي

### الحل

```kotlin
// استخدام صور مضغوطة
BitmapFactory.Options().apply {
    inSampleSize = 2
    inPreferredConfig = Bitmap.Config.RGB_565
}
```

## خيارات بديلة

### 1. استخدام Unicode Characters

```kotlin
// رموز Unicode للأشكال
♠ (U+2660) - Spade
♥ (U+2665) - Heart
♦ (U+2666) - Diamond
♣ (U+2663) - Club
```

### 2. استخدام مكتبات Compose

```kotlin
// استخدام Painter مخصص
val painter = painterResource(id = R.drawable.card_back)
```

### 3. رسم الأوراق برمجياً

```kotlin
Canvas(modifier = Modifier.size(100.dp, 150.dp)) {
    // رسم الورقة يدويًا باستخدام DrawScope
    drawRect(Color.White, size = size)
    // رسم الأشكال والنصوص
}
```

## الملفات المتضمنة

✅ جميع الملفات اللازمة موجودة في المشروع  
✅ الصور تُنشأ ديناميكياً من Compose  
✅ لا حاجة لتحميل صور إضافية  
✅ التطبيق يعمل بدون صور خارجية  

## التوافقية

يعمل التطبيق بدون أي صور خارجية:
- ✅ أجهزة قديمة (API 24+)
- ✅ أجهزة حديثة (API 34)
- ✅ أجهزة بمواصفات منخفضة
- ✅ أجهزة بمواصفات عالية

---

**المشروع كامل وجاهز للاستخدام الفوري!** 🎉

