package com.glory.game.ui;



import android.app.Activity;

import android.app.Dialog;

import android.graphics.Color;

import android.graphics.Typeface;

import android.graphics.drawable.ColorDrawable;

import android.graphics.drawable.GradientDrawable;

import android.os.Bundle;

import android.util.DisplayMetrics;

import android.util.TypedValue;

import android.view.Gravity;

import android.view.View;

import android.view.ViewGroup;

import android.view.Window;

import android.widget.Button;

import android.widget.HorizontalScrollView;

import android.widget.LinearLayout;

import android.widget.ScrollView;

import android.widget.SeekBar;

import android.widget.Switch;

import android.widget.TextView;

import android.widget.Toast;



import java.util.ArrayList;

import java.util.List;



public class HudEditorDialog extends Dialog {
    public static native void setChatEditorOpen(boolean open);



    public enum Category {

        ALL("همه", "All"),

        RADAR("🗺️ رادار", "Radar"),

        HEALTH_ARMOR("❤️ سلامت و زره", "Health & Armour"),

        WEAPON("اسلحه و مهمات", "Weapons"),

        STATUS("وضعیت و نمایش", "Status & Info"),

        BUTTONS("دکمه‌ها و کنترل", "Buttons & Controls"),

        VEHICLE("خودرو و سرعت‌سنج", "Vehicle & Speed");



        final String faName;

        final String enName;



        Category(String fa, String en) {

            this.faName = fa;

            this.enName = en;

        }

    }



    private static class ElementDef {

        final int id;

        final Category category;

        final String titleFa;

        final String titleEn;

        final int maxX;

        final int maxY;

        final int maxScale;



        ElementDef(int id, Category category, String titleFa, String titleEn, int maxX, int maxY, int maxScale) {

            this.id = id;

            this.category = category;

            this.titleFa = titleFa;

            this.titleEn = titleEn;

            this.maxX = maxX;

            this.maxY = maxY;

            this.maxScale = maxScale;

        }

    }



    private static class Row {

        ElementDef def;

        View cardView;

        int x, y, scale;

        SeekBar barX, barY, barScale;

        SeekBar barExtra1, barExtra2;

        TextView valBadge;

        TextView labelX, labelY, labelScale;

    }



    private static final ElementDef[] ELEMENTS = {

            // Radar

            new ElementDef(Hud.HUD_RADAR, Category.RADAR, "رادار و مینی‌مپ", "Radar / MiniMap", 1920, 1080, 250),



            // Health & Armour

            new ElementDef(Hud.HUD_HP, Category.HEALTH_ARMOR, "سلامتی و درصد", "Health & Percentage", 1920, 1080, 400),

            new ElementDef(Hud.HUD_ARMOR, Category.HEALTH_ARMOR, "جلیقه و درصد", "Armour & Percentage", 1920, 1080, 400),



            // Weapon & Ammo

            new ElementDef(Hud.HUD_FIST, Category.WEAPON, "سلاح و شمارنده تیر", "Weapon & Ammo", 1920, 1080, 300),



            // Status & Money & Info

            new ElementDef(Hud.HUD_MONEY, Category.STATUS, "میزان پول", "Money Display", 1920, 1080, 450),

            new ElementDef(Hud.HUD_WANTED, Category.STATUS, "ستاره‌های تعقیب", "Wanted Stars", 1920, 1080, 450),

            new ElementDef(Hud.HUD_EAT, Category.STATUS, "میزان گرسنگی", "Hunger / Eat Bar", 1920, 1080, 350),

            new ElementDef(Hud.HUD_KILL_LIST, Category.STATUS, "کیل‌لیست (کشتارها)", "Kill List", 1920, 1080, 250),

            new ElementDef(Hud.HUD_CHAT, Category.STATUS, "تنظیمات پنجره چت", "Chat Window", 1000, 600, 70),



            // Buttons & Controls

            new ElementDef(Hud.HUD_SHOOT_BTN, Category.BUTTONS, "دکمه شلیک نشونه‌گیری", "Aim Fire Button", 1920, 1080, 250),

            new ElementDef(Hud.HUD_TAB, Category.BUTTONS, "دکمه Tab (لیست بازیکنان)", "Tab Button", 1920, 1080, 300),

            new ElementDef(Hud.HUD_VOICE_BTN, Category.BUTTONS, "دکمه ویس‌چت", "Voice Chat Button", 1920, 1080, 300),

            new ElementDef(Hud.HUD_ENTER_CAR_BTN, Category.BUTTONS, "دکمه ورود به خودرو", "Enter Car Button", 1920, 1080, 300),

            new ElementDef(Hud.HUD_CAMERA_BTN, Category.BUTTONS, "دکمه تعویض دوربین", "Camera Switch Button", 1920, 1080, 300),

    };



    private final Hud hud;

    private final Row[] rows = new Row[ELEMENTS.length];

    private final float[] initialX = new float[Hud.HUD_ELEMENTS];

    private final float[] initialY = new float[Hud.HUD_ELEMENTS];

    private final float[] initialScale = new float[Hud.HUD_ELEMENTS];

    private final float[] initialScaleY = new float[Hud.HUD_ELEMENTS];

    private final float[] initialSens = new float[Hud.HUD_ELEMENTS];



    private LinearLayout root;

    private View speedometerCard;

    private final List<Button> tabButtons = new ArrayList<>();

    private Category currentCategory = Category.ALL;



    public HudEditorDialog(Activity activity, Hud hud) {

        super(activity);

        this.hud = hud;



        for (int i = 0; i < Hud.HUD_ELEMENTS; i++) {

            initialX[i] = hud.getHudPositionX(i);

            initialY[i] = hud.getHudPositionY(i);

            initialScale[i] = hud.getHudScale(i);

            initialScaleY[i] = hud.hudScaleY[i];

            initialSens[i] = hud.hudSensitivity[i];

        }

    }



    @Override
    protected void onStop() {
        super.onStop();
        try { setChatEditorOpen(false); } catch (Throwable ignored) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        try { setChatEditorOpen(true); } catch (Throwable ignored) {}

        requestWindowFeature(Window.FEATURE_NO_TITLE);



        if (getWindow() != null) {

            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            getWindow().setDimAmount(0.12f);

        }



        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();

        int width = Math.min((int) (metrics.widthPixels * 0.82f), dp(780));

        int height = Math.min((int) (metrics.heightPixels * 0.94f), dp(620));



        // Root Container

        root = new LinearLayout(getContext());

        root.setOrientation(LinearLayout.VERTICAL);

        root.setPadding(dp(16), dp(14), dp(16), dp(14));



        GradientDrawable rootBg = new GradientDrawable();

        rootBg.setColor(0xFF14171A);

        rootBg.setCornerRadius(dp(16));

        rootBg.setStroke(dp(1), 0x33FFFFFF);

        root.setBackground(rootBg);



        // 1. Header Bar

        LinearLayout header = new LinearLayout(getContext());

        header.setOrientation(LinearLayout.HORIZONTAL);

        header.setGravity(Gravity.CENTER_VERTICAL);

        header.setPadding(0, 0, 0, dp(8));



        TextView title = new TextView(getContext());

        title.setText("⚙️ ویرایشگر و تنظیمات جامع هود (HUD Editor)");

        title.setTextColor(Color.WHITE);

        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        title.setTypeface(Typeface.DEFAULT_BOLD);

        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        header.addView(title);



        TextView hint = new TextView(getContext());

        hint.setText("تغییرات به صورت زنده اعمال می‌شوند");

        hint.setTextColor(0xFF81C784);

        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        header.addView(hint);



        root.addView(header);



        // 2. Horizontal Category Tabs Bar

        HorizontalScrollView tabScroll = new HorizontalScrollView(getContext());

        tabScroll.setHorizontalScrollBarEnabled(false);

        tabScroll.setPadding(0, 0, 0, dp(10));



        LinearLayout tabLayout = new LinearLayout(getContext());

        tabLayout.setOrientation(LinearLayout.HORIZONTAL);

        tabLayout.setGravity(Gravity.CENTER_VERTICAL);



        tabButtons.clear();

        for (Category cat : Category.values()) {

            Button btn = new Button(getContext());

            btn.setText(cat.faName);

            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

            btn.setPadding(dp(12), dp(4), dp(12), dp(4));

            btn.setAllCaps(false);



            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(

                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));

            btnLp.setMargins(0, 0, dp(6), 0);

            btn.setLayoutParams(btnLp);



            btn.setOnClickListener(v -> selectCategory(cat));

            tabButtons.add(btn);

            tabLayout.addView(btn);

        }



        tabScroll.addView(tabLayout);

        root.addView(tabScroll);



        updateTabStyles();



        // 3. Scrollable Elements List

        ScrollView scroll = new ScrollView(getContext());

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(

                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);

        scroll.setLayoutParams(scrollParams);

        scroll.setVerticalScrollBarEnabled(true);



        LinearLayout list = new LinearLayout(getContext());

        list.setOrientation(LinearLayout.VERTICAL);

        list.setPadding(0, dp(4), 0, dp(4));



        for (int i = 0; i < ELEMENTS.length; i++) {

            ElementDef def = ELEMENTS[i];

            Row row = new Row();

            row.def = def;



            row.x = Math.round(hud.getHudPositionX(def.id));

            row.y = Math.round(hud.getHudPositionY(def.id));

            row.scale = Math.round(hud.getHudScale(def.id));



            row.cardView = buildElementCard(row);

            rows[i] = row;

            list.addView(row.cardView);

        }



        // Add Launcher Speedometer card

        speedometerCard = buildSpeedometerCard();

        list.addView(speedometerCard);



        scroll.addView(list);

        root.addView(scroll);



        // 4. Bottom Action Buttons Bar

        LinearLayout actions = new LinearLayout(getContext());

        actions.setOrientation(LinearLayout.HORIZONTAL);

        actions.setGravity(Gravity.CENTER_VERTICAL);

        actions.setPadding(0, dp(8), 0, dp(2));



        Button resetBtn = createActionButton("بازنشانی (Reset)", 0xFF37474F, 0xFFECEFF1);

        Button closeBtn = createActionButton("انصراف (Cancel)", 0xFF424242, 0xFFE0E0E0);

        Button saveBtn = createActionButton("ذخیره تغییرات (Save)", 0xFF2E7D32, 0xFFFFFFFF);



        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, dp(38), 1.0f);

        resetParams.setMargins(0, 0, dp(6), 0);

        resetBtn.setLayoutParams(resetParams);



        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(0, dp(38), 1.0f);

        closeParams.setMargins(0, 0, dp(6), 0);

        closeBtn.setLayoutParams(closeParams);



        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(38), 1.3f);

        saveBtn.setLayoutParams(saveParams);



        resetBtn.setOnClickListener(v -> {

            hud.resetHudConfig();

            for (Row r : rows) {

                r.x = Math.round(hud.getHudPositionX(r.def.id));

                r.y = Math.round(hud.getHudPositionY(r.def.id));

                r.scale = Math.round(hud.getHudScale(r.def.id));

                if (r.barX != null) r.barX.setProgress(Math.max(0, Math.min(r.def.maxX, r.x)));

                if (r.barY != null) r.barY.setProgress(Math.max(0, Math.min(r.def.maxY, r.y)));

                if (r.barScale != null) {

                    if (r.def.id == Hud.HUD_CHAT) {
                        r.barScale.setProgress(Math.max(0, Math.min(70 - 30, r.scale - 30)));
                    } else {
                        r.barScale.setProgress(Math.max(20, Math.min(r.def.maxScale, r.scale)));
                    }
                }
                if (r.def.id == Hud.HUD_CHAT) {
                    if (r.barExtra2 != null) {
                        int font = Math.round(hud.hudSensitivity[Hud.HUD_CHAT]);
                        r.barExtra2.setProgress(Math.max(0, Math.min(36 - 14, font - 14)));
                    }
                }
                updateRowLabels(r);

            }

            Toast.makeText(getContext(), "تنظیمات هود به حالت پیش‌فرض بازگشت", Toast.LENGTH_SHORT).show();

        });



        closeBtn.setOnClickListener(v -> {

            for (int j = 0; j < Hud.HUD_ELEMENTS; j++) {

                hud.hudScaleY[j] = initialScaleY[j];

                hud.hudSensitivity[j] = initialSens[j];

                hud.applyHudElement(j, initialX[j], initialY[j], initialScale[j]);

            }

            dismiss();

        });



        saveBtn.setOnClickListener(v -> {

            for (Row r : rows) {

                hud.applyHudElement(r.def.id, r.x, r.y, r.scale);

            }

            hud.saveHudConfig();

            Toast.makeText(getContext(), "تنظیمات هود ذخیره شد", Toast.LENGTH_SHORT).show();

            dismiss();

        });



        actions.addView(resetBtn);

        actions.addView(closeBtn);

        actions.addView(saveBtn);



        root.addView(actions);



        setContentView(root, new ViewGroup.LayoutParams(width, height));

    }



    private void selectCategory(Category category) {

        currentCategory = category;

        updateTabStyles();



        for (Row r : rows) {

            if (r == null || r.cardView == null) continue;

            boolean show = (category == Category.ALL || r.def.category == category);

            r.cardView.setVisibility(show ? View.VISIBLE : View.GONE);

        }



        if (speedometerCard != null) {

            boolean showSpeed = (category == Category.ALL || category == Category.VEHICLE);

            speedometerCard.setVisibility(showSpeed ? View.VISIBLE : View.GONE);

        }

    }



    private void updateTabStyles() {

        Category[] cats = Category.values();

        for (int i = 0; i < cats.length && i < tabButtons.size(); i++) {

            Button btn = tabButtons.get(i);

            boolean active = (cats[i] == currentCategory);



            GradientDrawable gd = new GradientDrawable();

            gd.setCornerRadius(dp(17));



            if (active) {

                gd.setColor(0xFF8E24AA);

                btn.setTextColor(Color.WHITE);

                btn.setTypeface(Typeface.DEFAULT_BOLD);

            } else {

                gd.setColor(0xFF252A30);

                gd.setStroke(dp(1), 0x22FFFFFF);

                btn.setTextColor(0xFFB0BEC5);

                btn.setTypeface(Typeface.DEFAULT);

            }

            btn.setBackground(gd);

        }

    }



    private View buildElementCard(Row row) {

        LinearLayout card = new LinearLayout(getContext());

        card.setOrientation(LinearLayout.VERTICAL);

        card.setPadding(dp(12), dp(10), dp(12), dp(10));



        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(

                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        lp.setMargins(0, 0, 0, dp(10));

        card.setLayoutParams(lp);



        GradientDrawable cardBg = new GradientDrawable();

        cardBg.setColor(0xFF1E2227);

        cardBg.setCornerRadius(dp(10));

        cardBg.setStroke(dp(1), 0x26FFFFFF);

        card.setBackground(cardBg);



        // Header Row: [Category Badge] [Title Fa / En] [Live Value Badge]

        LinearLayout header = new LinearLayout(getContext());

        header.setOrientation(LinearLayout.HORIZONTAL);

        header.setGravity(Gravity.CENTER_VERTICAL);



        TextView catBadge = new TextView(getContext());

        catBadge.setText(row.def.category.faName);

        catBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

        catBadge.setTextColor(0xFFFFCC80);

        catBadge.setPadding(dp(6), dp(2), dp(6), dp(2));

        GradientDrawable badgeBg = new GradientDrawable();

        badgeBg.setColor(0x33FFA726);

        badgeBg.setCornerRadius(dp(4));

        catBadge.setBackground(badgeBg);

        header.addView(catBadge);



        TextView title = new TextView(getContext());

        title.setText(" " + row.def.titleFa + " (" + row.def.titleEn + ")");

        title.setTextColor(Color.WHITE);

        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        title.setTypeface(Typeface.DEFAULT_BOLD);

        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        title.setPadding(dp(6), 0, dp(6), 0);

        header.addView(title);



        row.valBadge = new TextView(getContext());

        row.valBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        row.valBadge.setTextColor(0xFFCE93D8);

        row.valBadge.setTypeface(Typeface.MONOSPACE);

        header.addView(row.valBadge);



        card.addView(header);



        // Special layout for Chat Window: EXACTLY TWO SLIDERS
        if (row.def.id == Hud.HUD_CHAT) {
            int curWidth = Math.max(30, Math.min(70, row.scale > 0 ? row.scale : 45));
            row.barScale = addRangeSlider(card, "??? ??", 30, 70, curWidth, "%", (val) -> {
                row.scale = val;
                hud.hudScaleX[Hud.HUD_CHAT] = val;
                updateRowLabels(row);
                hud.applyChatHud();
            });

            int curFont = (hud.hudSensitivity[Hud.HUD_CHAT] > 0f) ? Math.round(hud.hudSensitivity[Hud.HUD_CHAT]) : 20;
            curFont = Math.max(14, Math.min(36, curFont));
            row.barExtra2 = addDeferredRangeSlider(card, "?????? ???? ??", 14, 36, curFont, " pt", (val) -> {
                hud.hudSensitivity[Hud.HUD_CHAT] = val;
                updateRowLabels(row);
                hud.applyChatHud();
            });

            updateRowLabels(row);
            return card;
        }

        // Sliders: X, Y, Scale

        row.barX = addSlider(card, "موقعیت افقی (X)", row.def.maxX, row.x, (val) -> {

            row.x = val;

            updateRowLabels(row);

            hud.applyHudElement(row.def.id, row.x, row.y, row.scale);

        });



        row.barY = addSlider(card, "موقعیت عمودی (Y)", row.def.maxY, row.y, (val) -> {

            row.y = val;

            updateRowLabels(row);

            hud.applyHudElement(row.def.id, row.x, row.y, row.scale);

        });



        row.barScale = addScaleSlider(card, "مقیاس / اندازه (Scale)", row.def.maxScale, row.scale, (val) -> {

            row.scale = val;

            updateRowLabels(row);

            hud.applyHudElement(row.def.id, row.x, row.y, row.scale);

        });



        // Sensitivity slider for shoot button

        if (row.def.id == Hud.HUD_SHOOT_BTN) {

            int sensVal = (int) hud.hudSensitivity[Hud.HUD_SHOOT_BTN];

            addSlider(card, "حساسیت چرخش دکمه شلیک (Sensitivity)", 800, sensVal, (val) -> {

                hud.hudSensitivity[Hud.HUD_SHOOT_BTN] = val;

                hud.applyHudElement(row.def.id, row.x, row.y, row.scale);

            });

        }



        updateRowLabels(row);



        return card;

    }



    private View buildSpeedometerCard() {

        LinearLayout card = new LinearLayout(getContext());

        card.setOrientation(LinearLayout.VERTICAL);

        card.setPadding(dp(12), dp(10), dp(12), dp(10));



        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(

                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        lp.setMargins(0, 0, 0, dp(10));

        card.setLayoutParams(lp);



        GradientDrawable cardBg = new GradientDrawable();

        cardBg.setColor(0xFF1E2227);

        cardBg.setCornerRadius(dp(10));

        cardBg.setStroke(dp(1), 0x26FFFFFF);

        card.setBackground(cardBg);



        LinearLayout row = new LinearLayout(getContext());

        row.setOrientation(LinearLayout.HORIZONTAL);

        row.setGravity(Gravity.CENTER_VERTICAL);



        LinearLayout textLayout = new LinearLayout(getContext());

        textLayout.setOrientation(LinearLayout.VERTICAL);

        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));



        TextView title = new TextView(getContext());

        title.setText("🚗 کیلومتر شمار پیش‌فرض لانچر (Launcher Speedometer)");

        title.setTextColor(Color.WHITE);

        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        title.setTypeface(Typeface.DEFAULT_BOLD);

        textLayout.addView(title);



        TextView desc = new TextView(getContext());

        desc.setText("غیرفعال جهت جلوگیری از تداخل با سرعت‌سنج سرور (پیشنهادی)");

        desc.setTextColor(0xFF9E9E9E);

        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        desc.setPadding(0, dp(2), 0, 0);

        textLayout.addView(desc);



        row.addView(textLayout);



        Switch sw = new Switch(getContext());

        sw.setChecked(Speedometer.s_bEnableSpeedometer);

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {

            Speedometer.s_bEnableSpeedometer = isChecked;

            desc.setText(isChecked ? "فعال (سرعت‌سنج عقربه‌ای لانچر نمایش داده می‌شود)" :

                    "غیرفعال جهت جلوگیری از تداخل با سرعت‌سنج سرور (پیشنهادی)");

            desc.setTextColor(isChecked ? 0xFF81C784 : 0xFF9E9E9E);

        });

        row.addView(sw);



        card.addView(row);

        return card;

    }



    private interface OnValueChanged {

        void onVal(int val);

    }



    private SeekBar addSlider(LinearLayout card, String name, int max, int current, OnValueChanged cb) {

        LinearLayout row = new LinearLayout(getContext());

        row.setOrientation(LinearLayout.HORIZONTAL);

        row.setGravity(Gravity.CENTER_VERTICAL);

        row.setPadding(0, dp(4), 0, 0);



        TextView label = new TextView(getContext());

        label.setText(name + ": " + current);

        label.setTextColor(0xFFB0BEC5);

        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f));

        row.addView(label);



        SeekBar bar = new SeekBar(getContext());

        bar.setMax(max);

        bar.setProgress(Math.max(0, Math.min(max, current)));

        bar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f));



        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (fromUser) {

                    label.setText(name + ": " + progress);

                    cb.onVal(progress);

                }

            }



            @Override

            public void onStartTrackingTouch(SeekBar seekBar) {

                if (root != null) {

                    root.animate().alpha(0.08f).setDuration(100).start();

                }

                if (getWindow() != null) {

                    getWindow().setDimAmount(0.0f);

                }

            }



            @Override

            public void onStopTrackingTouch(SeekBar seekBar) {

                if (root != null) {

                    root.animate().alpha(1.0f).setDuration(160).start();

                }

                if (getWindow() != null) {

                    getWindow().setDimAmount(0.12f);

                }

            }

        });



        row.addView(bar);

        card.addView(row);

        return bar;

    }



    private SeekBar addScaleSlider(LinearLayout card, String name, int maxScale, int currentScale, OnValueChanged cb) {

        LinearLayout row = new LinearLayout(getContext());

        row.setOrientation(LinearLayout.HORIZONTAL);

        row.setGravity(Gravity.CENTER_VERTICAL);

        row.setPadding(0, dp(4), 0, 0);



        int cur = (currentScale > 0) ? currentScale : 100;



        TextView label = new TextView(getContext());

        label.setText(name + ": " + cur + "%");

        label.setTextColor(0xFFB0BEC5);

        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f));

        row.addView(label);



        SeekBar bar = new SeekBar(getContext());

        bar.setMax(maxScale);

        bar.setProgress(Math.max(20, Math.min(maxScale, cur)));

        bar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f));



        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                int valid = Math.max(20, progress);

                if (fromUser) {

                    label.setText(name + ": " + valid + "%");

                    cb.onVal(valid);

                }

            }



            @Override

            public void onStartTrackingTouch(SeekBar seekBar) {

                if (root != null) {

                    root.animate().alpha(0.08f).setDuration(100).start();

                }

                if (getWindow() != null) {

                    getWindow().setDimAmount(0.0f);

                }

            }



            @Override

            public void onStopTrackingTouch(SeekBar seekBar) {

                if (root != null) {

                    root.animate().alpha(1.0f).setDuration(160).start();

                }

                if (getWindow() != null) {

                    getWindow().setDimAmount(0.12f);

                }

            }

        });



        row.addView(bar);

        card.addView(row);

        return bar;

    }



    private SeekBar addRangeSlider(LinearLayout card, String name, int min, int max, int current, String unit, OnValueChanged cb) {

        LinearLayout row = new LinearLayout(getContext());

        row.setOrientation(LinearLayout.HORIZONTAL);

        row.setGravity(Gravity.CENTER_VERTICAL);

        row.setPadding(0, dp(4), 0, 0);



        TextView label = new TextView(getContext());

        label.setText(name + ": " + current + unit);

        label.setTextColor(0xFFB0BEC5);

        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f));

        row.addView(label);



        SeekBar bar = new SeekBar(getContext());

        bar.setMax(max - min);

        bar.setProgress(Math.max(0, Math.min(max - min, current - min)));

        bar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f));



        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                int actual = min + progress;

                label.setText(name + ": " + actual + unit);

                if (fromUser) {

                    cb.onVal(actual);

                }

            }



            @Override

            public void onStartTrackingTouch(SeekBar seekBar) {

                if (root != null) {

                    root.animate().alpha(0.08f).setDuration(100).start();

                }

                if (getWindow() != null) {

                    getWindow().setDimAmount(0.0f);

                }

            }



            @Override

            public void onStopTrackingTouch(SeekBar seekBar) {

                if (root != null) {

                    root.animate().alpha(1.0f).setDuration(160).start();

                }

                if (getWindow() != null) {

                    getWindow().setDimAmount(0.12f);

                }

            }

        });



        row.addView(bar);

        card.addView(row);

        return bar;

    }



    private SeekBar addDeferredRangeSlider(LinearLayout card, String name, int min, int max, int current, String unit, OnValueChanged cb) {

        LinearLayout row = new LinearLayout(getContext());

        row.setOrientation(LinearLayout.HORIZONTAL);

        row.setGravity(Gravity.CENTER_VERTICAL);

        row.setPadding(0, dp(4), 0, 0);



        TextView label = new TextView(getContext());

        label.setText(name + ": " + current + unit);

        label.setTextColor(0xFFB0BEC5);

        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f));

        row.addView(label);



        SeekBar bar = new SeekBar(getContext());

        bar.setMax(max - min);

        bar.setProgress(Math.max(0, Math.min(max - min, current - min)));

        bar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f));



        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                int actual = min + progress;

                label.setText(name + ": " + actual + unit);

            }



            @Override

            public void onStartTrackingTouch(SeekBar seekBar) {

                if (root != null) {

                    root.animate().alpha(0.08f).setDuration(100).start();

                }

                if (getWindow() != null) {

                    getWindow().setDimAmount(0.0f);

                }

            }



            @Override

            public void onStopTrackingTouch(SeekBar seekBar) {

                if (root != null) {

                    root.animate().alpha(1.0f).setDuration(160).start();

                }

                if (getWindow() != null) {

                    getWindow().setDimAmount(0.12f);

                }

                int actual = min + seekBar.getProgress();

                cb.onVal(actual);

            }

        });



        row.addView(bar);

        card.addView(row);

        return bar;

    }



    private void updateRowLabels(Row row) {

        if (row.valBadge != null) {

            if (row.def.id == Hud.HUD_CHAT) {

                int font = (hud.hudSensitivity[Hud.HUD_CHAT] > 0f) ? Math.round(hud.hudSensitivity[Hud.HUD_CHAT]) : 20;
                row.valBadge.setText("???: " + row.scale + "% | ????: " + font + "pt");

            } else {

                row.valBadge.setText("X:" + row.x + " Y:" + row.y + " S:" + row.scale + "%");

            }

        }

    }



    private Button createActionButton(String text, int bgColor, int textColor) {

        Button b = new Button(getContext());

        b.setText(text);

        b.setTextColor(textColor);

        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        b.setTypeface(Typeface.DEFAULT_BOLD);

        b.setAllCaps(false);



        GradientDrawable bg = new GradientDrawable();

        bg.setColor(bgColor);

        bg.setCornerRadius(dp(8));

        b.setBackground(bg);

        return b;

    }



    private int dp(int v) {

        return (int) TypedValue.applyDimension(

                TypedValue.COMPLEX_UNIT_DIP, v, getContext().getResources().getDisplayMetrics());

    }

}

