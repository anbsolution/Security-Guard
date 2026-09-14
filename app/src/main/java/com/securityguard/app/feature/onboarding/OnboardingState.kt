package com.securityguard.app.feature.onboarding

enum class OnboardingStep(val index: Int, val title: String) {
    WELCOME(0, "Welcome"), PROFILE(1, "Guard Profile"), SALARY(2, "Salary"), WEEKLY_OFF(3, "Weekly Off"),
    PIN(4, "PIN"), SHIFT(5, "Shifts"), CHECKPOINTS(6, "Checkpoints"), ROUND_SETTINGS(7, "Round Settings"),
    ALERTS(8, "Alerts"), PERMISSIONS(9, "Permissions"), REVIEW(10, "Review")
}
