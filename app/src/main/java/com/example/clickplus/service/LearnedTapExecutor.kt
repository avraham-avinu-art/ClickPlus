package com.example.clickplus.service



class LearnedTapExecutor(private val service: AccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())

    private fun pointForCurrentOrientation(
        config: KeyActionConfig,
        second: Boolean,
    ): Pair<Float, Float> {
        val defaultX = if (second) config.screenTapSecondXRatio else config.screenTapXRatio
        val defaultY = if (second) config.screenTapSecondYRatio else config.screenTapYRatio
        val meta = AdvancedRuleRepository(service).getRuleMetadata(config.id)
        if (!meta.useOrientationSpecificPosition) return defaultX to defaultY

        val landscape = service.resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val x = if (second) {
            if (landscape) meta.landscapeSecondX else meta.portraitSecondX
        } else {
            if (landscape) meta.landscapeX else meta.portraitX
        }
        val y = if (second) {
            if (landscape) meta.landscapeSecondY else meta.portraitSecondY
        } else {
            if (landscape) meta.landscapeY else meta.portraitY
        }
        return if (x >= 0f && y >= 0f) x to y else defaultX to defaultY
    }

    fun executeAppTap(config: KeyActionConfig): ActionExecutionResult {
        val targetPackage = config.screenTapPackage.trim()
        if (targetPackage.isBlank()) {
            return ActionExecutionResult.failure("לא נבחרה אפליקציה לביצוע הלחיצה")
        }

        val point = pointForCurrentOrientation(config, second = false)
        val xRatio = point.first.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
        val yRatio = point.second.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
        if (xRatio == null || yRatio == null || config.screenTapXRatio < 0f || config.screenTapYRatio < 0f) {
            return ActionExecutionResult.failure("לא נלמד מיקום לחיצה תקין באפליקציה")
        }

        val intent = service.packageManager.getLaunchIntentForPackage(targetPackage)
            ?: return ActionExecutionResult.failure("אפליקציית היעד אינה מותקנת או שאין לה מסך פתיחה")

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        service.startActivity(intent)

        val executionId = java.util.UUID.randomUUID().toString()
        scheduleTapWhenAppIsVisible(config, targetPackage, xRatio, yRatio, executionId)
        return ActionExecutionResult.pending(
            "האפליקציה נפתחה; ממתינים להופעתה על המסך ואז תתבצע הלחיצה",
            executionId,
        )
    }

    fun executeMultiPointTap(config: KeyActionConfig): ActionExecutionResult {
        val targetPackage = config.screenTapPackage.trim()
        if (targetPackage.isBlank()) return ActionExecutionResult.failure("לא נבחרה אפליקציה")
        val first = pointForCurrentOrientation(config, second = false)
        val second = pointForCurrentOrientation(config, second = true)
        val x1 = first.first
        val y1 = first.second
        val x2 = second.first
        val y2 = second.second
        if (x1 < 0f || y1 < 0f || x2 < 0f || y2 < 0f) {
            return ActionExecutionResult.failure("לא נלמדו שתי נקודות לחיצה")
        }
        val intent = service.packageManager.getLaunchIntentForPackage(targetPackage)
            ?: return ActionExecutionResult.failure("אפליקציית היעד אינה מותקנת")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        service.startActivity(intent)
        val executionId = java.util.UUID.randomUUID().toString()
        scheduleTwoTapsWhenAppIsVisible(config, targetPackage, x1, y1, x2, y2, executionId)
        return ActionExecutionResult.pending(
            "האפליקציה נפתחה; שתי הלחיצות יתבצעו כשהיא תהיה בחזית",
            executionId,
        )
    }

    private fun scheduleTwoTapsWhenAppIsVisible(
        config: KeyActionConfig,
        targetPackage: String,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        executionId: String,
    ) {
        val startedAt = System.currentTimeMillis()
        val check = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() - startedAt >= 6000L) {
                    logAppTapResult(config, targetPackage, false, "האפליקציה לא הופיעה בחזית בתוך 6 שניות", executionId)
                    return
                }
                if (service.rootInActiveWindow?.packageName?.toString().orEmpty() == targetPackage) {
                    val meta = AdvancedRuleRepository(service).getRuleMetadata(config.id)
                    val delayMs = (if (meta.delayMs > 0L) meta.delayMs else AppPreferencesRepository.actionDelaySnapshot(service))
                        .coerceIn(0L, 10_000L)
                    if (delayMs == 0L) {
                        dispatchTwoTaps(config, targetPackage, x1, y1, x2, y2, executionId)
                    } else {
                        handler.postDelayed(
                            { dispatchTwoTaps(config, targetPackage, x1, y1, x2, y2, executionId) },
                            delayMs,
                        )
                    }
                } else handler.postDelayed(this, 150L)
            }
        }
        handler.post(check)
    }

    private fun dispatchTwoTaps(
        config: KeyActionConfig,
        targetPackage: String,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        executionId: String,
    ) {
        val metrics = service.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val path = Path().apply {
            moveTo((x1 * width).coerceIn(0f, width - 1f), (y1 * height).coerceIn(0f, height - 1f))
        }
        val gesture1 = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()
        service.dispatchGesture(
            gesture1,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    val delay = config.screenTapIntervalMs.coerceIn(500L, 10_000L)
                    handler.postDelayed({
                        if (service.rootInActiveWindow?.packageName?.toString().orEmpty() != targetPackage) {
                            logAppTapResult(
                                config,
                                targetPackage,
                                false,
                                "אפליקציית היעד יצאה מהחזית לפני הלחיצה השנייה",
                                executionId,
                            )
                            return@postDelayed
                        }
                        val path2 = Path().apply {
                            moveTo((x2 * width).coerceIn(0f, width - 1f), (y2 * height).coerceIn(0f, height - 1f))
                        }
                        val gesture2 = GestureDescription.Builder()
                            .addStroke(GestureDescription.StrokeDescription(path2, 0L, 60L))
                            .build()
                        service.dispatchGesture(
                            gesture2,
                            object : AccessibilityService.GestureResultCallback() {
                                override fun onCompleted(gestureDescription: GestureDescription?) {
                                    logAppTapResult(config, targetPackage, true, "שתי הלחיצות בוצעו בהצלחה", executionId)
                                }
                                override fun onCancelled(gestureDescription: GestureDescription?) {
                                    logAppTapResult(config, targetPackage, false, "הלחיצה השנייה בוטלה על ידי Android", executionId)
                                }
                            },
                            handler,
                        )
                    }, delay)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    logAppTapResult(config, targetPackage, false, "הלחיצה הראשונה בוטלה על ידי Android", executionId)
                }
            },
            handler,
        )
    }

    private fun scheduleTapWhenAppIsVisible(
        config: KeyActionConfig,
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
        executionId: String,
    ) {
        val startedAt = System.currentTimeMillis()
        val timeoutMs = 6000L

        val check = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() - startedAt >= timeoutMs) {
                    logAppTapResult(config, targetPackage, false, "האפליקציה נפתחה, אך לא זוהתה בחזית בתוך 6 שניות", executionId)
                    return
                }

                val foregroundPackage = service.rootInActiveWindow?.packageName?.toString().orEmpty()
                if (foregroundPackage == targetPackage) {
                    val meta = AdvancedRuleRepository(service).getRuleMetadata(config.id)
                    val delayMs = (if (meta.delayMs > 0L) meta.delayMs else AppPreferencesRepository.actionDelaySnapshot(service))
                        .coerceIn(0L, 10_000L)
                    val run = {
                        val count = config.screenTapCount.coerceIn(1, 10)
                        if (count == 1) {
                            dispatchLearnedTap(config, targetPackage, xRatio, yRatio, executionId)
                        } else {
                            dispatchRepeatedTaps(
                                config,
                                targetPackage,
                                xRatio,
                                yRatio,
                                count,
                                1,
                                executionId,
                            )
                        }
                    }
                    if (delayMs == 0L) run() else handler.postDelayed(run, delayMs)
                } else {
                    handler.postDelayed(this, 150L)
                }
            }
        }
        handler.post(check)
    }

    private fun dispatchRepeatedTaps(
        config: KeyActionConfig,
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
        count: Int,
        index: Int,
        executionId: String,
    ) {
        if (service.rootInActiveWindow?.packageName?.toString().orEmpty() != targetPackage) {
            logAppTapResult(
                config,
                targetPackage,
                false,
                "אפליקציית היעד יצאה מהחזית במהלך סדרת הלחיצות",
                executionId,
            )
            return
        }

        val metrics = service.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val x = (xRatio * width).coerceIn(0f, width - 1f)
        val y = (yRatio * height).coerceIn(0f, height - 1f)
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()

        runCatching {
            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (index >= count) {
                            logAppTapResult(
                                config,
                                targetPackage,
                                true,
                                "כל " + count + " הלחיצות בוצעו בהצלחה",
                                executionId,
                            )
                        } else {
                            handler.postDelayed(
                                {
                                    dispatchRepeatedTaps(
                                        config,
                                        targetPackage,
                                        xRatio,
                                        yRatio,
                                        count,
                                        index + 1,
                                        executionId,
                                    )
                                },
                                config.screenTapIntervalMs.coerceIn(500L, 10_000L),
                            )
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        logAppTapResult(
                            config,
                            targetPackage,
                            false,
                            "לחיצה מספר " + index + " בוטלה על ידי Android",
                            executionId,
                        )
                    }
                },
                handler,
            )
        }.onFailure { error ->
            logAppTapResult(
                config,
                targetPackage,
                false,
                "לא ניתן לשלוח לחיצה מספר " + index + ": " +
                    (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName),
                executionId,
            )
        }
    }

    private fun dispatchLearnedTap(
        config: KeyActionConfig,
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
        executionId: String,
    ) {
        val metrics = service.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val x = (xRatio * width).coerceIn(0f, width - 1f)
        val y = (yRatio * height).coerceIn(0f, height - 1f)

        if (service.rootInActiveWindow?.packageName?.toString().orEmpty() != targetPackage) {
            logAppTapResult(
                config,
                targetPackage,
                false,
                "אפליקציית היעד כבר אינה בחזית בזמן הלחיצה",
                executionId,
            )
            return
        }

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()

        runCatching {
            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        logAppTapResult(
                            config,
                            targetPackage,
                            true,
                            "הלחיצה בוצעה ב-X ${(xRatio * 100f).toInt()}% · Y ${(yRatio * 100f).toInt()}%",
                            executionId,
                        )
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        logAppTapResult(config, targetPackage, false, "Android ביטל את הלחיצה באפליקציית היעד", executionId)
                    }
                },
                handler,
            )
        }.onFailure { error ->
            logAppTapResult(
                config,
                targetPackage,
                false,
                "לא ניתן לשלוח את הלחיצה: " + (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName),
                executionId,
            )
        }
    }

    private fun logAppTapResult(
        config: KeyActionConfig,
        targetPackage: String,
        success: Boolean,
        detail: String,
        executionId: String,
    ) {
        AdvancedRuleRepository.updateActionLog(
            context = service,
            logId = executionId,
            appPackage = targetPackage,
            success = success,
            detail = detail,
        )
    }
}
