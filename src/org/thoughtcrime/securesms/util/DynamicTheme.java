package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Intent;

import network.loki.messenger.R;

/**
 * @deprecated Use one of the Theme.Session.DayNight.*
 * (or Theme.TextSecure.DayNight.* for old Signal activities)
 * app themes to support dark/light modes.
 */
@Deprecated
public class DynamicTheme {

  public static final String DARK  = "dark";
  public static final String LIGHT = "light";

  private int currentTheme;

  public void onCreate(Activity activity) {
    currentTheme = getSelectedTheme(activity);
    activity.setTheme(currentTheme);
  }

  public void onResume(Activity activity) {
    if (currentTheme != getSelectedTheme(activity)) {
      Intent intent = activity.getIntent();
      activity.finish();
      OverridePendingTransition.invoke(activity);
      activity.startActivity(intent);
      OverridePendingTransition.invoke(activity);
    }
  }

  protected int getSelectedTheme(Activity activity) {
    // For all legacy (Signal) activities we lock on to the Signal dedicated theme.
    return R.style.Theme_TextSecure_DayNight;
  }

  private static final class OverridePendingTransition {
    static void invoke(Activity activity) {
      activity.overridePendingTransition(0, 0);
    }
  }
}
