package com.example.model

data class IranianBank(
    val id: String,
    val nameFa: String,
    val nameEn: String = nameFa,
    val cardPrefix: String = "",
    val primaryColorHex: Long = 0xFF1976D2,
    val isCustom: Boolean = false
)

object IranianBanks {
    val defaultList = listOf(
        IranianBank("melli", "بانک ملی ایران", "Bank Melli Iran", "603799", 0xFF0D47A1),
        IranianBank("mellat", "بانک ملت", "Bank Mellat", "610433", 0xFFC2185B),
        IranianBank("saderat", "بانک صادرات ایران", "Bank Saderat Iran", "603769", 0xFF1B5E20),
        IranianBank("tejarat", "بانک تجارت", "Tejarat Bank", "585983", 0xFF00695C),
        IranianBank("sepah", "بانک سپه", "Bank Sepah", "589210", 0xFFE65100),
        IranianBank("refah", "بانک رفاه کارگران", "Refah Bank", "589463", 0xFF311B92),
        IranianBank("pasargad", "بانک پاسارگاد", "Pasargad Bank", "502229", 0xFFFFB300),
        IranianBank("saman", "بانک سامان", "Saman Bank", "621986", 0xFF0288D1),
        IranianBank("parsian", "بانک پارسیان", "Parsian Bank", "622106", 0xFF880E4F),
        IranianBank("eghtesad_novin", "بانک اقتصاد نوین", "EN Bank", "627412", 0xFF5D4037),
        IranianBank("shahr", "بانک شهر", "Shahr Bank", "504706", 0xFFD32F2F),
        IranianBank("dey", "بانک دی", "Dey Bank", "502938", 0xFF00897B),
        IranianBank("gardeshgari", "بانک گردشگری", "Tourism Bank", "505416", 0xFFF57C00),
        IranianBank("karafarin", "بانک کارآفرین", "Karafarin Bank", "627488", 0xFF455A64),
        IranianBank("sina", "بانک سینا", "Sina Bank", "639346", 0xFF388E3C),
        IranianBank("iran_zamin", "بانک ایران زمین", "Iran Zamin Bank", "505785", 0xFF6A1B9A),
        IranianBank("khavarmianeh", "بانک خاورمیانه", "Middle East Bank", "585947", 0xFF37474F),
        IranianBank("ayandeh", "بانک آینده", "Ayandeh Bank", "636214", 0xFF795548),
        IranianBank("mehr_iran", "بانک قرض‌الحسنه مهر ایران", "Qarz Al-Hasaneh Mehr Iran", "606373", 0xFF2E7D32),
        IranianBank("resalat", "بانک قرض‌الحسنه رسالت", "Resalat Bank", "504172", 0xFF1565C0),
        IranianBank("keshavarzi", "بانک کشاورزی", "Keshavarzi Bank", "603770", 0xFF2E7D32),
        IranianBank("maskan", "بانک مسکن", "Bank Maskan", "628023", 0xFFFF8F00),
        IranianBank("post_bank", "پست بانک ایران", "Post Bank of Iran", "627760", 0xFF00838F),
        IranianBank("tosee_taavon", "بانک توسعه تعاون", "Tose'e Ta'avon Bank", "502908", 0xFF0097A7),
        IranianBank("other", "سایر بانک‌ها و مؤسسات", "Other Bank / Institution", "", 0xFF546E7A)
    )

    private val customBanks = mutableListOf<IranianBank>()

    val all: List<IranianBank>
        get() = synchronized(customBanks) { defaultList + customBanks.toList() }

    fun getCustomBanks(): List<IranianBank> = synchronized(customBanks) { customBanks.toList() }

    fun setCustomBanks(banks: List<IranianBank>) {
        synchronized(customBanks) {
            customBanks.clear()
            customBanks.addAll(banks)
        }
    }

    fun addCustomBank(nameFa: String, nameEn: String = nameFa, cardPrefix: String = "", colorHex: Long = 0xFF00897B): IranianBank {
        val id = "custom_" + System.currentTimeMillis()
        val bank = IranianBank(id, nameFa, nameEn, cardPrefix, colorHex, isCustom = true)
        synchronized(customBanks) {
            customBanks.add(bank)
        }
        return bank
    }

    fun findById(id: String): IranianBank? = all.firstOrNull { it.id == id }
    fun findByCardPrefix(prefix: String): IranianBank? = all.firstOrNull { it.cardPrefix.isNotEmpty() && prefix.startsWith(it.cardPrefix) }
}
