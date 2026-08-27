package com.example.utils

object AppStrings {

    // General & Branding
    fun appName(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "AF store"
        AppLanguage.ENGLISH -> "AF store"
        AppLanguage.ARABIC -> "AF store"
    }

    fun appTagline(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Point de vente & Gestion de stock"
        AppLanguage.ENGLISH -> "Point of Sale & Inventory System"
        AppLanguage.ARABIC -> "نظام نقاط البيع وإدارة المخزون والمبيعات"
    }

    fun currency(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "EGP"
        AppLanguage.ENGLISH -> "EGP"
        AppLanguage.ARABIC -> "ج.م"
    }

    fun searchPlaceholder(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Rechercher un produit ou scanner..."
        AppLanguage.ENGLISH -> "Search product or scan barcode..."
        AppLanguage.ARABIC -> "ابحث عن صنف أو امسح الباركود..."
    }

    fun all(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Tous"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    fun save(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Enregistrer"
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.ARABIC -> "حفظ"
    }

    fun cancel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Annuler"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.ARABIC -> "إلغاء"
    }

    fun close(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Fermer"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.ARABIC -> "إغلاق"
    }

    fun delete(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Supprimer"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.ARABIC -> "حذف"
    }

    fun edit(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Modifier"
        AppLanguage.ENGLISH -> "Edit"
        AppLanguage.ARABIC -> "تعديل"
    }

    fun confirm(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Confirmer"
        AppLanguage.ENGLISH -> "Confirm"
        AppLanguage.ARABIC -> "تأكيد"
    }

    // Navigation Tabs
    fun tabPos(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Point de Vente"
        AppLanguage.ENGLISH -> "POS"
        AppLanguage.ARABIC -> "نقطة البيع"
    }

    fun tabCustomers(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Clients"
        AppLanguage.ENGLISH -> "Customers"
        AppLanguage.ARABIC -> "الزبائن"
    }

    fun tabInventory(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Stock"
        AppLanguage.ENGLISH -> "Inventory"
        AppLanguage.ARABIC -> "المخزون"
    }

    fun tabReports(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Tableau de bord"
        AppLanguage.ENGLISH -> "Dashboard"
        AppLanguage.ARABIC -> "لوحة التحكم"
    }

    fun tabSettings(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Paramètres"
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.ARABIC -> "الإعدادات"
    }

    // Authentication & Users
    fun login(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Connexion"
        AppLanguage.ENGLISH -> "Sign In"
        AppLanguage.ARABIC -> "تسجيل الدخول"
    }

    fun logout(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Déconnexion"
        AppLanguage.ENGLISH -> "Logout"
        AppLanguage.ARABIC -> "تسجيل الخروج"
    }

    fun username(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nom d'utilisateur"
        AppLanguage.ENGLISH -> "Username"
        AppLanguage.ARABIC -> "اسم المستخدم"
    }

    fun password(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Mot de passe"
        AppLanguage.ENGLISH -> "Password"
        AppLanguage.ARABIC -> "كلمة المرور"
    }

    fun roleAdmin(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Administrateur"
        AppLanguage.ENGLISH -> "Admin"
        AppLanguage.ARABIC -> "مدير النظام"
    }

    fun roleSeller(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Vendeur (Caisse & Stock)"
        AppLanguage.ENGLISH -> "Seller (POS & Stock)"
        AppLanguage.ARABIC -> "بائع (نقطة البيع والمخزون)"
    }

    fun demoAccountsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Comptes de démonstration :"
        AppLanguage.ENGLISH -> "Quick Demo Accounts:"
        AppLanguage.ARABIC -> "حسابات تجريبية سريعة للبدء:"
    }

    fun invalidLogin(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Identifiants invalides, veuillez vérifier le nom et mot de passe"
        AppLanguage.ENGLISH -> "Invalid credentials, please check username and password"
        AppLanguage.ARABIC -> "بيانات الدخول غير صحيحة، تحقق من اسم المستخدم وكلمة المرور"
    }

    // POS & Cart
    fun cartTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Panier de vente"
        AppLanguage.ENGLISH -> "Sales Cart"
        AppLanguage.ARABIC -> "سلة المبيعات"
    }

    fun cartEmpty(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Le panier est vide"
        AppLanguage.ENGLISH -> "Cart is empty"
        AppLanguage.ARABIC -> "السلة فارغة"
    }

    fun cartItemsCount(count: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "$count articles"
        AppLanguage.ENGLISH -> "$count items"
        AppLanguage.ARABIC -> "$count أصناف"
    }

    fun piece(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "pièce"
        AppLanguage.ENGLISH -> "pc"
        AppLanguage.ARABIC -> "قطعة"
    }

    fun carton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "carton"
        AppLanguage.ENGLISH -> "box"
        AppLanguage.ARABIC -> "عبوة"
    }

    fun subtotal(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Sous-total"
        AppLanguage.ENGLISH -> "Subtotal"
        AppLanguage.ARABIC -> "المجموع الفرعي"
    }

    fun discount(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Remise"
        AppLanguage.ENGLISH -> "Discount"
        AppLanguage.ARABIC -> "الخصم"
    }

    fun total(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Total net"
        AppLanguage.ENGLISH -> "Total"
        AppLanguage.ARABIC -> "الإجمالي الصافي"
    }

    fun checkoutCash(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Paiement Espèces"
        AppLanguage.ENGLISH -> "Pay Cash"
        AppLanguage.ARABIC -> "دفع نقدي (كاش)"
    }

    fun checkoutDebt(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Vente à Crédit / Dette"
        AppLanguage.ENGLISH -> "Credit Sale (Debt)"
        AppLanguage.ARABIC -> "تسجيل كدين (آجل)"
    }

    fun clearCart(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Vider le panier"
        AppLanguage.ENGLISH -> "Clear Cart"
        AppLanguage.ARABIC -> "إفراغ السلة"
    }

    fun printInvoice(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Imprimer reçu"
        AppLanguage.ENGLISH -> "Print Receipt"
        AppLanguage.ARABIC -> "طباعة الفاتورة"
    }

    fun previewInvoice(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Aperçu de la facture"
        AppLanguage.ENGLISH -> "Invoice Preview"
        AppLanguage.ARABIC -> "معاينة الفاتورة"
    }

    fun scanBarcode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Scanner Code-barres"
        AppLanguage.ENGLISH -> "Barcode Scanner"
        AppLanguage.ARABIC -> "ماسح الباركود"
    }

    fun selectCustomerLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Client :"
        AppLanguage.ENGLISH -> "Customer:"
        AppLanguage.ARABIC -> "الزبون:"
    }

    fun generalCustomer(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Client comptoir (Général)"
        AppLanguage.ENGLISH -> "Walk-in Customer (General)"
        AppLanguage.ARABIC -> "عميل عام (نقدي)"
    }

    fun todaySalesTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Ventes du jour :"
        AppLanguage.ENGLISH -> "Today's Sales:"
        AppLanguage.ARABIC -> "مبيعات اليوم:"
    }

    fun quickSell(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Vente directe"
        AppLanguage.ENGLISH -> "Quick Sell"
        AppLanguage.ARABIC -> "بيع مباشر"
    }

    fun addToCart(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "+ Panier"
        AppLanguage.ENGLISH -> "+ Cart"
        AppLanguage.ARABIC -> "+ للسلة"
    }

    fun addCartonToCart(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "+ Carton"
        AppLanguage.ENGLISH -> "+ Carton"
        AppLanguage.ARABIC -> "+ عبوة"
    }

    // Inventory & Products
    fun addProduct(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Ajouter un produit"
        AppLanguage.ENGLISH -> "Add Product"
        AppLanguage.ARABIC -> "إضافة صنف جديد"
    }

    fun editProduct(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Modifier le produit"
        AppLanguage.ENGLISH -> "Edit Product"
        AppLanguage.ARABIC -> "تعديل بيانات الصنف"
    }

    fun productName(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nom du produit"
        AppLanguage.ENGLISH -> "Product Name"
        AppLanguage.ARABIC -> "اسم المنتج"
    }

    fun purchasePrice(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Prix d'achat"
        AppLanguage.ENGLISH -> "Purchase Price"
        AppLanguage.ARABIC -> "سعر الشراء (التكلفة)"
    }

    fun sellingPrice(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Prix de vente"
        AppLanguage.ENGLISH -> "Selling Price"
        AppLanguage.ARABIC -> "سعر البيع للجمهور"
    }

    fun stockQuantity(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Quantité en stock"
        AppLanguage.ENGLISH -> "Stock Quantity"
        AppLanguage.ARABIC -> "الكمية المتاحة بالمخزن"
    }

    fun unitsPerCarton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Unités par carton"
        AppLanguage.ENGLISH -> "Units per Carton"
        AppLanguage.ARABIC -> "سعة العبوة (عدد القطع)"
    }

    fun barcode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Code-barres"
        AppLanguage.ENGLISH -> "Barcode"
        AppLanguage.ARABIC -> "رمز الباركود"
    }

    fun unitBarcode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Code-barres de la pièce / unité"
        AppLanguage.ENGLISH -> "Unit / Piece Barcode"
        AppLanguage.ARABIC -> "باركود القطعة / الوحدة الفردية"
    }

    fun cartonBarcode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Code-barres du carton complet"
        AppLanguage.ENGLISH -> "Full Carton Barcode"
        AppLanguage.ARABIC -> "باركود العبوة الكبرى"
    }

    fun generateUnitBarcode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Générer code pièce"
        AppLanguage.ENGLISH -> "Generate Unit Code"
        AppLanguage.ARABIC -> "توليد كود قطعة"
    }

    fun generateCartonBarcode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Générer code carton"
        AppLanguage.ENGLISH -> "Generate Carton Code"
        AppLanguage.ARABIC -> "توليد كود العبوة"
    }

    fun supplyCartons(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Approvisionner en cartons"
        AppLanguage.ENGLISH -> "Restock Cartons"
        AppLanguage.ARABIC -> "توريد عبوات جديدة"
    }

    fun trashBin(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Corbeille des produits"
        AppLanguage.ENGLISH -> "Product Recycle Bin"
        AppLanguage.ARABIC -> "سلة محذوفات الأصناف"
    }

    fun restore(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Restaurer"
        AppLanguage.ENGLISH -> "Restore"
        AppLanguage.ARABIC -> "استعادة"
    }

    fun permanentDelete(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Supprimer définitivement"
        AppLanguage.ENGLISH -> "Permanently Delete"
        AppLanguage.ARABIC -> "حذف نهائي"
    }

    fun lowStockWarning(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Stock faible !"
        AppLanguage.ENGLISH -> "Low Stock!"
        AppLanguage.ARABIC -> "⚠️ المخزون منخفض!"
    }

    fun outOfStock(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Rupture de stock"
        AppLanguage.ENGLISH -> "Out of Stock"
        AppLanguage.ARABIC -> "نفد من المخزون"
    }

    // Product Images
    fun productImage(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Photo du produit"
        AppLanguage.ENGLISH -> "Product Photo"
        AppLanguage.ARABIC -> "صورة المنتج"
    }

    fun chooseFromGallery(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Choisir de la galerie"
        AppLanguage.ENGLISH -> "Choose from Gallery"
        AppLanguage.ARABIC -> "اختيار من المعرض"
    }

    fun capturePhoto(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Prendre une photo"
        AppLanguage.ENGLISH -> "Take Photo"
        AppLanguage.ARABIC -> "التقاط بالكاميرا"
    }

    fun removePhoto(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Supprimer l'image"
        AppLanguage.ENGLISH -> "Remove Image"
        AppLanguage.ARABIC -> "حذف الصورة"
    }

    fun changePhoto(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Changer l'image"
        AppLanguage.ENGLISH -> "Change Image"
        AppLanguage.ARABIC -> "تغيير الصورة"
    }

    fun imageStoredNotice(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Image enregistrée dans la mémoire de l'appareil 💾"
        AppLanguage.ENGLISH -> "Image saved to device storage 💾"
        AppLanguage.ARABIC -> "تم حفظ الصورة في ذاكرة الهاتف 💾"
    }

    // Profit Margin Calculation
    fun profitMargin(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Marge bénéficiaire"
        AppLanguage.ENGLISH -> "Profit Margin"
        AppLanguage.ARABIC -> "هامش الربح"
    }

    fun directPriceMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Prix direct"
        AppLanguage.ENGLISH -> "Direct Price"
        AppLanguage.ARABIC -> "سعر مباشر"
    }

    fun marginPercentMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Pourcentage %"
        AppLanguage.ENGLISH -> "Percentage %"
        AppLanguage.ARABIC -> "نسبة مئوية %"
    }

    fun marginAmountMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Montant du profit"
        AppLanguage.ENGLISH -> "Profit Amount"
        AppLanguage.ARABIC -> "مبلغ الربح"
    }

    fun setProfitBy(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Définir le prix de vente par:"
        AppLanguage.ENGLISH -> "Set selling price by:"
        AppLanguage.ARABIC -> "طريقة تحديد سعر البيع:"
    }

    fun profitPercentLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Pourcentage de marge (%)"
        AppLanguage.ENGLISH -> "Margin Percentage (%)"
        AppLanguage.ARABIC -> "نسبة الربح المستهدفة (%)"
    }

    fun profitAmountLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Montant du bénéfice net (EGP)"
        AppLanguage.ENGLISH -> "Net Profit Amount (EGP)"
        AppLanguage.ARABIC -> "مبلغ الربح الصافي للقطعة (ج.م)"
    }

    fun calculatedSellingPrice(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Prix de vente calculé"
        AppLanguage.ENGLISH -> "Calculated Selling Price"
        AppLanguage.ARABIC -> "سعر البيع الناتج"
    }

    fun quickMarginPresets(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Marges rapides:"
        AppLanguage.ENGLISH -> "Quick margins:"
        AppLanguage.ARABIC -> "نسب سريعة:"
    }

    fun quickAmountPresets(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Montants rapides:"
        AppLanguage.ENGLISH -> "Quick amounts:"
        AppLanguage.ARABIC -> "مبالغ سريعة:"
    }

    // Carton / Box Registration
    fun registerByCarton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Enregistrer par Carton 📦"
        AppLanguage.ENGLISH -> "Register by Carton / Box 📦"
        AppLanguage.ARABIC -> "تسجيل بالعبوة الكبرى / طرد 📦"
    }

    fun registerByPiece(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Enregistrer par Pièce 🏷️"
        AppLanguage.ENGLISH -> "Register by Unit / Piece 🏷️"
        AppLanguage.ARABIC -> "تسجيل بالقطعة الفردية 🏷️"
    }

    fun cartonCostLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Prix d'achat du carton (EGP)"
        AppLanguage.ENGLISH -> "Carton Purchase Price (EGP)"
        AppLanguage.ARABIC -> "سعر شراء العبوة (ج.م)"
    }

    fun unitsInCartonLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nombre d'unités dans le carton"
        AppLanguage.ENGLISH -> "Units inside carton / box"
        AppLanguage.ARABIC -> "عدد الوحدات داخل العبوة"
    }

    fun unitCostCalculated(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Coût unitaire calculé"
        AppLanguage.ENGLISH -> "Calculated Unit Cost"
        AppLanguage.ARABIC -> "تكلفة القطعة المحسوبة"
    }

    fun cartonCountStock(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nombre de cartons"
        AppLanguage.ENGLISH -> "Number of cartons"
        AppLanguage.ARABIC -> "عدد العبوات"
    }

    fun looseUnitsStock(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Pièces individuelles"
        AppLanguage.ENGLISH -> "Loose pieces"
        AppLanguage.ARABIC -> "قطع فردية"
    }

    fun totalUnitsStock(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Total des pièces en stock"
        AppLanguage.ENGLISH -> "Total units in stock"
        AppLanguage.ARABIC -> "إجمالي القطع في المخزن"
    }

    fun cartonSellingPrice(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Prix de vente du carton"
        AppLanguage.ENGLISH -> "Carton Selling Price"
        AppLanguage.ARABIC -> "سعر بيع العبوة"
    }

    // Customers & Debts
    fun customerDirectoryTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Répertoire des Clients"
        AppLanguage.ENGLISH -> "Customer Directory"
        AppLanguage.ARABIC -> "دليل الزبائن والعملاء"
    }

    fun customerDirectoryDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Gestion des contacts et historique pour un encaissement rapide"
        AppLanguage.ENGLISH -> "Save contacts & details for ultra-fast checkout"
        AppLanguage.ARABIC -> "حفظ جهات الاتصال والبيانات لتسريع عمليات البيع والدفع"
    }

    fun fastCheckout(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Vente Rapide"
        AppLanguage.ENGLISH -> "Fast Checkout"
        AppLanguage.ARABIC -> "بيع سريع"
    }

    fun favoriteCustomer(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Client favori / régulier"
        AppLanguage.ENGLISH -> "Star / Regular Customer"
        AppLanguage.ARABIC -> "زبون مميز / دائم"
    }

    fun email(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Adresse e-mail"
        AppLanguage.ENGLISH -> "Email Address"
        AppLanguage.ARABIC -> "البريد الإلكتروني"
    }

    fun searchCustomerHint(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Recherche (nom, tél, e-mail, adresse)..."
        AppLanguage.ENGLISH -> "Search by name, phone, email, address..."
        AppLanguage.ARABIC -> "بحث بالاسم، رقم الهاتف، البريد، أو العنوان..."
    }

    fun filterFavorites(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Favoris ⭐"
        AppLanguage.ENGLISH -> "Favorites ⭐"
        AppLanguage.ARABIC -> "المميزون ⭐"
    }

    fun addCustomer(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nouveau client"
        AppLanguage.ENGLISH -> "Add Customer"
        AppLanguage.ARABIC -> "إضافة زبون جديد"
    }

    fun newCustomerQuick(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "+ Nouveau client"
        AppLanguage.ENGLISH -> "+ New Customer"
        AppLanguage.ARABIC -> "+ زبون جديد"
    }

    fun selectCustomerQuick(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Choisir client"
        AppLanguage.ENGLISH -> "Select Customer"
        AppLanguage.ARABIC -> "تحديد زبون"
    }

    fun createCustomerAndSelect(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Créer & Sélectionner"
        AppLanguage.ENGLISH -> "Create & Select"
        AppLanguage.ARABIC -> "إنشاء وتحديد الزبون"
    }

    fun customerName(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nom du client"
        AppLanguage.ENGLISH -> "Customer Name"
        AppLanguage.ARABIC -> "اسم الزبون"
    }

    fun phone(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Numéro de téléphone"
        AppLanguage.ENGLISH -> "Phone Number"
        AppLanguage.ARABIC -> "رقم الهاتف"
    }

    fun address(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Adresse"
        AppLanguage.ENGLISH -> "Address"
        AppLanguage.ARABIC -> "العنوان"
    }

    fun notes(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Notes & Remarques"
        AppLanguage.ENGLISH -> "Notes & Remarks"
        AppLanguage.ARABIC -> "ملاحظات"
    }

    fun balanceDebt(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Dette / Solde"
        AppLanguage.ENGLISH -> "Current Debt / Balance"
        AppLanguage.ARABIC -> "الرصيد / الدين المستحق"
    }

    fun totalPurchases(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Total des achats"
        AppLanguage.ENGLISH -> "Total Purchases"
        AppLanguage.ARABIC -> "إجمالي المشتريات"
    }

    fun recordPayment(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Règlement / Paiement"
        AppLanguage.ENGLISH -> "Record Payment"
        AppLanguage.ARABIC -> "سداد دفعة نقدية"
    }

    fun addManualDebt(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Ajouter une dette"
        AppLanguage.ENGLISH -> "Add Debt"
        AppLanguage.ARABIC -> "إضافة دين يدوي"
    }

    fun statement(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Relevé de compte"
        AppLanguage.ENGLISH -> "Account Statement"
        AppLanguage.ARABIC -> "كشف حساب المعاملات"
    }

    fun totalCustomersDebt(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Total dettes clients :"
        AppLanguage.ENGLISH -> "Total Customers Debt:"
        AppLanguage.ARABIC -> "إجمالي ديون الزبائن:"
    }

    // Reports
    fun reportsSummary(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Rapport & Analyse financière"
        AppLanguage.ENGLISH -> "Financial & Sales Reports"
        AppLanguage.ARABIC -> "التقارير والتحليلات المالية"
    }

    fun weekly(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Hebdomadaire"
        AppLanguage.ENGLISH -> "Weekly"
        AppLanguage.ARABIC -> "أسبوعي"
    }

    fun monthly(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Mensuel"
        AppLanguage.ENGLISH -> "Monthly"
        AppLanguage.ARABIC -> "شهري"
    }

    fun yearly(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Annuel"
        AppLanguage.ENGLISH -> "Yearly"
        AppLanguage.ARABIC -> "سنوي"
    }

    fun revenue(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Chiffre d'affaires"
        AppLanguage.ENGLISH -> "Total Revenue"
        AppLanguage.ARABIC -> "إجمالي المبيعات"
    }

    fun netProfit(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Bénéfice net estimé"
        AppLanguage.ENGLISH -> "Estimated Net Profit"
        AppLanguage.ARABIC -> "صافي الأرباح التقديرية"
    }

    fun salesCount(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nombre de ventes"
        AppLanguage.ENGLISH -> "Sales Count"
        AppLanguage.ARABIC -> "عدد عمليات البيع"
    }

    fun topSellingProducts(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Produits les plus vendus"
        AppLanguage.ENGLISH -> "Top Selling Products"
        AppLanguage.ARABIC -> "الأصناف الأكثر مبيعاً"
    }

    // Settings
    fun languageSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "🌐 Langue de l'application"
        AppLanguage.ENGLISH -> "🌐 App Language"
        AppLanguage.ARABIC -> "🌐 لغة التطبيق (Language)"
    }

    fun appearanceSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "🎨 Apparence & Thèmes Multiples"
        AppLanguage.ENGLISH -> "🎨 Appearance & Multi-Themes"
        AppLanguage.ARABIC -> "🎨 المظهر وتخصيص الثيمات المتعددة"
    }

    fun themePaletteTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Palette de couleurs & Thème de marque"
        AppLanguage.ENGLISH -> "Color Palette & Brand Theme"
        AppLanguage.ARABIC -> "لوحة الألوان وثيم المتجر المخصص"
    }

    fun themePaletteDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Sélectionnez le style visuel de l'application"
        AppLanguage.ENGLISH -> "Select the visual theme for your POS"
        AppLanguage.ARABIC -> "اختر الثيم اللوني المفضل لنقاط البيع ولوحة التحكم"
    }

    fun displayModeTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Mode d'affichage"
        AppLanguage.ENGLISH -> "Display Mode"
        AppLanguage.ARABIC -> "نمط الإضاءة والعرض"
    }

    fun darkMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Mode sombre (Dark Mode)"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.ARABIC -> "الوضع الليلي (Dark Mode)"
    }

    fun darkModeDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Basculer l'interface en mode sombre"
        AppLanguage.ENGLISH -> "Switch interface to dark theme"
        AppLanguage.ARABIC -> "تبديل مظهر الواجهة للوضع الداكن"
    }

    fun printerSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "🖨️ Imprimante de reçus & Bluetooth"
        AppLanguage.ENGLISH -> "🖨️ Receipt Printer & Bluetooth"
        AppLanguage.ARABIC -> "🖨️ طابعة الفواتير والبلوتوث (POS Printer)"
    }

    fun connectedPrinter(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Imprimante configurée"
        AppLanguage.ENGLISH -> "Configured Printer"
        AppLanguage.ARABIC -> "الطابعة المتصلة"
    }

    fun changePrinter(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Changer"
        AppLanguage.ENGLISH -> "Change"
        AppLanguage.ARABIC -> "تغيير"
    }

    fun testPrint(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Imprimer un reçu de test"
        AppLanguage.ENGLISH -> "Print Test Receipt"
        AppLanguage.ARABIC -> "طباعة إيصال تجريبي"
    }

    fun dataBackupSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "💾 Gestion des données & Sauvegarde (CSV)"
        AppLanguage.ENGLISH -> "💾 Data Management & Backup (CSV)"
        AppLanguage.ARABIC -> "💾 إدارة البيانات والنسخ الاحتياطي (CSV)"
    }

    fun exportProductsCsv(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Exporter les produits en CSV"
        AppLanguage.ENGLISH -> "Export Products to CSV"
        AppLanguage.ARABIC -> "تصدير المنتجات إلى ملف CSV"
    }

    fun importProductsCsv(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Importer les produits depuis CSV"
        AppLanguage.ENGLISH -> "Import Products from CSV"
        AppLanguage.ARABIC -> "استيراد المنتجات من ملف CSV"
    }

    fun exportSalesCsv(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Exporter l'historique des ventes CSV"
        AppLanguage.ENGLISH -> "Export Sales History to CSV"
        AppLanguage.ARABIC -> "تصدير سجل المبيعات إلى CSV"
    }

    fun exportCustomersCsv(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Exporter les clients en CSV"
        AppLanguage.ENGLISH -> "Export Customers to CSV"
        AppLanguage.ARABIC -> "تصدير بيانات الزبائن إلى CSV"
    }

    fun importCustomersCsv(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Importer les clients depuis CSV"
        AppLanguage.ENGLISH -> "Import Customers from CSV"
        AppLanguage.ARABIC -> "استيراد بيانات الزبائن من CSV"
    }

    fun resetDatabase(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Réinitialiser la base de données"
        AppLanguage.ENGLISH -> "Reset Database"
        AppLanguage.ARABIC -> "إعادة ضبط قاعدة البيانات"
    }

    fun userManagementSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "👥 Gestion des utilisateurs & Droits"
        AppLanguage.ENGLISH -> "👥 User Management & Permissions"
        AppLanguage.ARABIC -> "👥 إدارة المستخدمين والصلاحيات"
    }

    fun addUser(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Ajouter utilisateur"
        AppLanguage.ENGLISH -> "Add User"
        AppLanguage.ARABIC -> "إضافة مستخدم"
    }

    // Camera Barcode Scanner
    fun scannerPrompt(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Pointez la caméra vers le code-barres"
        AppLanguage.ENGLISH -> "Point camera at product barcode"
        AppLanguage.ARABIC -> "وجّه الكاميرا نحو باركود المنتج"
    }

    fun autoAddMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Ajout automatique au panier"
        AppLanguage.ENGLISH -> "Auto-add to cart"
        AppLanguage.ARABIC -> "إضافة تلقائية للسلة"
    }

    fun manualCheckMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Vérification manuelle"
        AppLanguage.ENGLISH -> "Manual check"
        AppLanguage.ARABIC -> "فحص وتأكيد يدوي"
    }

    fun manualBarcodeInput(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Saisie manuelle du code-barres"
        AppLanguage.ENGLISH -> "Manual barcode entry"
        AppLanguage.ARABIC -> "إدخال كود يدوي / باركود تالف"
    }

    fun cameraPermissionRequired(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Autorisation de la caméra requise"
        AppLanguage.ENGLISH -> "Camera Permission Required"
        AppLanguage.ARABIC -> "إذن الكاميرا مطلوب"
    }

    fun grantPermission(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Autoriser la caméra"
        AppLanguage.ENGLISH -> "Grant Camera Permission"
        AppLanguage.ARABIC -> "منح إذن الكاميرا"
    }

    // Store & Inventory Alert Settings
    fun storeSettingsSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "🏪 Paramètres du Magasin"
        AppLanguage.ENGLISH -> "🏪 Store & Business Settings"
        AppLanguage.ARABIC -> "🏪 إعدادات المتجر وبيانات الفاتورة"
    }

    fun storeNameTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nom du magasin"
        AppLanguage.ENGLISH -> "Store Name"
        AppLanguage.ARABIC -> "اسم المتجر"
    }

    fun storeNameDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Nom affiché sur les factures et reçus"
        AppLanguage.ENGLISH -> "Name displayed on invoices and receipts"
        AppLanguage.ARABIC -> "الاسم المعتمد والمطبوع على الفواتير والإيصالات"
    }

    fun currencyTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Devise utilisée"
        AppLanguage.ENGLISH -> "Used Currency"
        AppLanguage.ARABIC -> "العملة المستخدمة"
    }

    fun currencyDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Symbole de monnaie pour les prix et rapports"
        AppLanguage.ENGLISH -> "Currency symbol for prices and sales"
        AppLanguage.ARABIC -> "رمز العملة الظاهر في الأسعار والفواتير والتقارير"
    }

    fun stockAlertsSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "🔔 Surveillance & Alertes de Stock"
        AppLanguage.ENGLISH -> "🔔 Stock Alerts & Monitoring"
        AppLanguage.ARABIC -> "🔔 مراقبة المخزون والتنبيهات"
    }

    fun lowStockAlertsToggle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Alertes de stock faible"
        AppLanguage.ENGLISH -> "Low Stock Alerts"
        AppLanguage.ARABIC -> "تفعيل تنبيهات نقص المخزون"
    }

    fun lowStockAlertsDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Afficher la bannière et mettre en évidence les produits presque épuisés"
        AppLanguage.ENGLISH -> "Show banner and highlight products running low"
        AppLanguage.ARABIC -> "إظهار شريط التحذير وتمييز الأصناف التي اقتربت من النفاد في المخزن ونقاط البيع"
    }

    fun lowStockThresholdTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Seuil d'alerte de stock"
        AppLanguage.ENGLISH -> "Low Stock Threshold"
        AppLanguage.ARABIC -> "حد التنبيه لنقص المخزون"
    }

    // USB & Built-in Thermal POS Printer
    fun usbPrinterSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "🔌 Imprimante Intégrée & USB"
        AppLanguage.ENGLISH -> "🔌 Built-in & USB Thermal Printer"
        AppLanguage.ARABIC -> "🔌 الطابعة المدمجة وطابعة الـ USB"
    }

    fun usbPrinterConfigButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Configurer USB / Intégrée"
        AppLanguage.ENGLISH -> "Setup USB / Built-in"
        AppLanguage.ARABIC -> "إعداد وفحص طابعة USB / المدمجة"
    }
}
