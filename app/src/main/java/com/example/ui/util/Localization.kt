package com.example.ui.util

enum class AppLanguage(val code: String, val displayName: String, val shortLabel: String) {
    ENGLISH("en", "English", "EN"),
    AMHARIC("am", "አማርኛ", "አማ")
}

enum class AppThemeMode(val displayNameEn: String, val displayNameAm: String) {
    SYSTEM("System Default", "የስርዓቱ ምርጫ"),
    LIGHT("Light Mode", "ብሩህ ገጽታ"),
    DARK("Dark Mode", "ጨለማ ገጽታ")
}

object AppStrings {
    fun appTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "የኔ ሱቅ"
        AppLanguage.AMHARIC -> "የኔ ሱቅ"
    }

    fun tabForm(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Data Entry"
        AppLanguage.AMHARIC -> "መረጃ ማስገቢያ"
    }

    fun tabInventory(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Inventory Log"
        AppLanguage.AMHARIC -> "የዕቃዎች ዝርዝር"
    }

    fun tabWebCode(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Apps Script"
        AppLanguage.AMHARIC -> "የድር ስክሪፕት"
    }

    fun tabPreview(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Web Preview"
        AppLanguage.AMHARIC -> "ቅድመ እይታ"
    }

    // Form Strings
    fun itemNo(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "1. Item No"
        AppLanguage.AMHARIC -> "1. የዕቃ ቁጥር"
    }

    fun autoIncrement(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Auto-increment"
        AppLanguage.AMHARIC -> "በቅደም ተከተል ጨምር"
    }

    fun autoIncrementHelp(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Automatically numbers next item sequentially"
        AppLanguage.AMHARIC -> "ቀጣዩን የዕቃ ቁጥር በቅደም ተከተል በራሱ ይሰጣል"
    }

    fun itemName(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "2. Item Name"
        AppLanguage.AMHARIC -> "2. የዕቃ ስም"
    }

    fun itemNamePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "e.g. Ergonomic Office Chair, Laptop..."
        AppLanguage.AMHARIC -> "ምሳሌ፡ የቢሮ ወንበር፣ ላፕቶፕ..."
    }

    fun itemQuantity(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "3. Quantity"
        AppLanguage.AMHARIC -> "3. ብዛት"
    }

    fun currencySymbol(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Birr"
        AppLanguage.AMHARIC -> "ብር"
    }

    fun formatPrice(amount: Double, lang: AppLanguage): String {
        return "${String.format(java.util.Locale.US, "%.2f", amount)} ${currencySymbol(lang)}"
    }

    fun itemPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "4. Unit Price (Birr)"
        AppLanguage.AMHARIC -> "4. የአንዱ ዋጋ (ብር)"
    }

    fun totalItemPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Total Item Price"
        AppLanguage.AMHARIC -> "ጠቅላላ የዕቃ ዋጋ"
    }

    fun liveCalculation(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Live Calculation"
        AppLanguage.AMHARIC -> "ቀጥታ ስሌት"
    }

    fun submitToGoogleSheet(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Submit to Google Sheet"
        AppLanguage.AMHARIC -> "ወደ ጉግል ሉህ መዝግብ"
    }

    fun submitting(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Submitting to Google Sheet..."
        AppLanguage.AMHARIC -> "በመመዝገብ ላይ..."
    }

    fun webAppUrlTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Google Apps Script Web App URL"
        AppLanguage.AMHARIC -> "የጉግል ሉህ የድር አድራሻ (URL)"
    }

    fun testConnection(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Test Connection"
        AppLanguage.AMHARIC -> "ግንኙነት ሞክር"
    }

    fun saveUrl(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Save URL"
        AppLanguage.AMHARIC -> "አድራሻ አስቀምጥ"
    }

    // List Screen
    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Search by name, item #, or ID..."
        AppLanguage.AMHARIC -> "በስም፣ በቁጥር ወይም በID ፈልግ..."
    }

    fun filterAll(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "All"
        AppLanguage.AMHARIC -> "ሁሉም"
    }

    fun filterSynced(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Synced"
        AppLanguage.AMHARIC -> "የተመሳሰለ"
    }

    fun filterPending(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Pending"
        AppLanguage.AMHARIC -> "ያልተላከ"
    }

    fun totalStockValueTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Total Stock Monetary Value"
        AppLanguage.AMHARIC -> "የስቶክ ጠቅላላ የገንዘብ ዋጋ"
    }

    fun totalStockValueSubtitle(itemCount: Int, unitCount: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Calculated sum of all $itemCount items ($unitCount units in stock)"
        AppLanguage.AMHARIC -> "የሁሉም $itemCount ዕቃዎች ጠቅላላ ድምር ($unitCount ፍሬዎች በስቶክ)"
    }

    fun totalItems(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Total Items"
        AppLanguage.AMHARIC -> "ጠቅላላ ዕቃዎች"
    }

    fun totalUnits(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Total Units"
        AppLanguage.AMHARIC -> "ጠቅላላ ብዛት"
    }

    fun totalValue(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Total Value"
        AppLanguage.AMHARIC -> "ጠቅላላ ዋጋ"
    }

    fun saveCsv(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Save CSV File"
        AppLanguage.AMHARIC -> "CSV ፋይል አስቀምጥ"
    }

    fun shareCsv(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Share CSV"
        AppLanguage.AMHARIC -> "CSV አጋራ"
    }

    fun syncPending(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.ENGLISH -> "Sync $count to Sheet"
        AppLanguage.AMHARIC -> "$count ያልተላኩትን ላክ"
    }

    fun allSynced(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "All synced to Sheet"
        AppLanguage.AMHARIC -> "ሁሉም ወደ ጉግል ሉህ ተልኳል"
    }

    fun clearAll(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear All"
        AppLanguage.AMHARIC -> "ሁሉንም አጽዳ"
    }

    fun edit(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Edit"
        AppLanguage.AMHARIC -> "አስተካክል"
    }

    fun delete(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.AMHARIC -> "ሰርዝ"
    }

    fun saveChanges(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Save Changes"
        AppLanguage.AMHARIC -> "ለውጦችን አስቀምጥ"
    }

    fun cancel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.AMHARIC -> "ይቅር"
    }

    fun noItemsYet(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No Inventory Items Yet"
        AppLanguage.AMHARIC -> "ምንም የተመዘገበ ዕቃ የለም"
    }

    fun noItemsMatch(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No Matching Items"
        AppLanguage.AMHARIC -> "የተገኘ ዕቃ የለም"
    }

    fun resetFilters(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Reset Filters"
        AppLanguage.AMHARIC -> "ማጣሪያውን አጽዳ"
    }

    fun importCsv(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Import CSV"
        AppLanguage.AMHARIC -> "CSV አስገባ"
    }

    fun importCsvDialogTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Bulk Import from CSV"
        AppLanguage.AMHARIC -> "ዕቃዎችን በጅምላ ከCSV አስገባ"
    }

    fun importModeAppend(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add to existing inventory"
        AppLanguage.AMHARIC -> "በነባሩ ስቶክ ላይ ጨምር"
    }

    fun importModeReplace(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Replace entire inventory"
        AppLanguage.AMHARIC -> "ነባሩን ስቶክ ሙሉ በሙሉ ተካ"
    }

    fun confirmImportButton(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Import $count Items"
        AppLanguage.AMHARIC -> "$count ዕቃዎችን አስገባ"
    }

    fun unitsLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "units"
        AppLanguage.AMHARIC -> "ፍሬ"
    }
}
