package com.example.model

enum class AccountType(val titleFa: String) {
    BANK("حساب بانکی"),
    CASH("وجه نقد و کیف پول"),
    SAVINGS("صندوق پس‌انداز / طلا"),
    CRYPTO("ارز دیجیتال / سرمایه‌گذاری")
}

enum class TransactionType(val titleFa: String) {
    EXPENSE("هزینه"),
    INCOME("درآمد"),
    TRANSFER("انتقال داخلی")
}

enum class DebtDirection(val titleFa: String) {
    I_OWE("بدهی من (طلب دیگران)"),
    OWED_TO_ME("طلب من (بدهی دیگران)")
}

enum class DebtStatus(val titleFa: String) {
    ACTIVE("در جریان"),
    PARTIALLY_PAID("پرداخت ناقص"),
    PAID("تسویه شده"),
    OVERDUE("سررسید گذشته")
}

enum class CheckDirection(val titleFa: String) {
    INCOMING("چک دریافتی"),
    OUTGOING("چک پرداختی (صادره)")
}

enum class CheckStatus(val titleFa: String) {
    RECEIVED_OR_ISSUED("ثبت شده / در انتظار سررسید"),
    PRESENTED("واگذار به بانک / ارائه شده"),
    CLEARED("پاس شده (نقد شده)"),
    BOUNCED("برگشت خورده"),
    CANCELLED("باطل شده")
}

enum class RecurringFrequency(val titleFa: String) {
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه"),
    YEARLY("سالانه")
}
