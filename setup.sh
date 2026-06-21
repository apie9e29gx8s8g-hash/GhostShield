#!/bin/bash
echo "[1/7] حذف Git القديم..."
rm -rf .git
echo "[2/7] تهيئة Git جديد..."
git init
echo "[3/7] إنشاء الفرع main..."
git checkout -b main
echo "[4/7] إضافة كل الملفات..."
git add .
echo "[5/7] عمل commit..."
git commit -m "Ultimate Shield"
echo "[6/7] إضافة remote..."
git remote add origin https://github.com/apie9e29gx8s8g-hash/GhostShield.git
echo "[7/7] رفع الكود للسيرفر (سيطلب اسم المستخدم وكلمة المرور)..."
git push -u origin main
echo "✅ تم كل شيء! روح GitHub وشوف Actions"
