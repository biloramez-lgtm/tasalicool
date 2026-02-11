# 🚀 تعليمات الرفع على GitHub من Termux

## ⚡ البدء السريع

```bash
# 1. استخرج الملف
cd ~/storage/downloads
unzip CardGames-Complete.zip
cd CardGames

# 2. ثبّت Git
pkg install git

# 3. أعدّ البيانات
git config --global user.name "اسمك"
git config --global user.email "بريدك@gmail.com"

# 4. هيّئ المستودع
git init
git add .
git commit -m "Initial commit: Card Games - Arabic Card Games App"

# 5. أضف الرابط
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/CardGames.git

# 6. رفع على GitHub
git push -u origin main
```

## 📋 الخطوات بالتفصيل

### 1️⃣ تثبيت Git

```bash
pkg update
pkg install git
git --version
```

### 2️⃣ إعداد البيانات

```bash
git config --global user.name "أحمد محمد"
git config --global user.email "ahmed@gmail.com"
git config --list
```

### 3️⃣ إنشاء مستودع محلي

```bash
cd ~/storage/downloads
unzip CardGames-Complete.zip
cd CardGames
git init
```

### 4️⃣ إضافة الملفات

```bash
git add .
git status  # تحقق من الملفات
```

### 5️⃣ أول Commit

```bash
git commit -m "Initial commit: Card Games Application
- لعبة 400 الكاملة
- دعم Bluetooth و Network
- واجهة Compose حديثة"
```

### 6️⃣ إنشاء مستودع على GitHub

1. اذهب إلى: https://github.com/new
2. اسم المستودع: `CardGames`
3. الوصف: `Arabic Card Games App - 400, Solitaire, Hand Game`
4. اختر Public أو Private
5. اضغط Create repository

### 7️⃣ ربط مع GitHub

```bash
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/CardGames.git
git remote -v  # تحقق من الربط
```

### 8️⃣ الرفع

```bash
git push -u origin main
```

عند الطلب:
- Username: اسمك على GitHub
- Password: Personal Access Token (ليس كلمة المرور!)

## 🔑 الحصول على Personal Access Token

1. اذهب إلى: https://github.com/settings/tokens
2. اضغط "Generate new token"
3. اختر "classic"
4. اختر الصلاحيات: `repo`
5. Copy الـ Token
6. احفظه في مكان آمن

## ✅ التحقق من النجاح

```bash
git log --oneline | head -5
```

اذهب إلى: https://github.com/YOUR_USERNAME/CardGames

يجب أن ترى جميع ملفاتك! 🎉

## 🔄 التحديثات المستقبلية

```bash
git add .
git commit -m "Update: وصف التغييرات"
git push origin main
```

## 🐛 حل المشاكل

### مشكلة: "fatal: Could not read from remote repository"

```bash
git remote -v
git remote remove origin
git remote add origin https://github.com/YOUR_USERNAME/CardGames.git
```

### مشكلة: "Authentication failed"

استخدم Personal Access Token بدلاً من كلمة المرور!

### مشكلة: "permission denied"

```bash
git config --global user.name
git config --global user.email
```

## 📱 الملفات المهمة

- `README.md` - التوثيق الرئيسي
- `build.gradle.kts` - إعدادات البناء
- `app/src/main/kotlin/` - الكود الرئيسي
- `app/build.gradle.kts` - إعدادات التطبيق

## ✨ النتيجة النهائية

بعد الرفع بنجاح:

✅ مستودع على GitHub  
✅ كود احترافي منظم  
✅ توثيق شاملة  
✅ تطبيق جاهز للتطوير  

## 🎯 الخطوة التالية

بعد الرفع، يمكنك:

1. فتح المشروع في Android Studio
2. تشغيل التطبيق
3. إضافة ميزات جديدة
4. رفع التحديثات

---

**استمتع بـ Git و GitHub!** 🚀

