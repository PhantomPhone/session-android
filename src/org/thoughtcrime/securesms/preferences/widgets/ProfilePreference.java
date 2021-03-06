package org.thoughtcrime.securesms.preferences.widgets;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

import network.loki.messenger.R;

public class ProfilePreference extends Preference {

  private View containerView;
  private ImageView avatarView;
  private TextView  profileNameView;
  private TextView  profileNumberView;
  private TextView  profileTagView;
  private String shortDeviceMnemonic;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public ProfilePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize();
  }

  public ProfilePreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  public ProfilePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public ProfilePreference(Context context) {
    super(context);
    initialize();
  }

  private void initialize() {
    setLayoutResource(R.layout.profile_preference_view);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder viewHolder) {
    super.onBindViewHolder(viewHolder);

    containerView     = viewHolder.itemView;
    avatarView        = (ImageView)viewHolder.findViewById(R.id.avatar);
    profileNameView   = (TextView)viewHolder.findViewById(R.id.profile_name);
    profileNumberView = (TextView)viewHolder.findViewById(R.id.number);
    profileTagView    = (TextView)viewHolder.findViewById(R.id.tag);

    refresh();
  }

  public void refresh() {
    if (profileNumberView == null) return;

    Context context = getContext();
    String userPublicKey = TextSecurePreferences.getLocalNumber(context);
    String userMasterPublicKey = TextSecurePreferences.getMasterHexEncodedPublicKey(context);
    String publicKeyToUse = userMasterPublicKey != null ? userMasterPublicKey : userPublicKey;
    final Address address = Address.fromSerialized(publicKeyToUse);
    final Recipient recipient = Recipient.from(context, address, false);
    final String displayName  = TextSecurePreferences.getProfileName(context);

    containerView.setOnLongClickListener(v -> {
      ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData clip = ClipData.newPlainText("Public Key", publicKeyToUse);
      clipboard.setPrimaryClip(clip);
      Toast.makeText(context, R.string.activity_settings_public_key_copied_message, Toast.LENGTH_SHORT).show();
      return true;
    });

    avatarView.setOutlineProvider(new ViewOutlineProvider() {

      @Override
      public void getOutline(View view, Outline outline) {
        outline.setOval(0, 0, view.getWidth(), view.getHeight());
      }
    });
    avatarView.setClipToOutline(true);

    Drawable fallback = recipient.getFallbackContactPhotoDrawable(context, false);
    GlideApp.with(getContext().getApplicationContext())
            .load(recipient.getContactPhoto())
            .fallback(fallback)
            .error(fallback)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(avatarView);


    if (!TextUtils.isEmpty(displayName)) {
      profileNameView.setText(displayName);
    }

    profileNameView.setVisibility(TextUtils.isEmpty(displayName) ? View.GONE : View.VISIBLE);
    profileNumberView.setText(address.toPhoneString());

    profileTagView.setVisibility(userMasterPublicKey == null ? View.GONE : View.VISIBLE);

    if (userMasterPublicKey != null && shortDeviceMnemonic == null) {
      shortDeviceMnemonic = "";
    }

    String tag = context.getResources().getString(R.string.activity_settings_linked_device_tag);
    profileTagView.setText(String.format(tag, shortDeviceMnemonic != null ? shortDeviceMnemonic : "-"));
  }
}
