package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.view.View;

import com.avocarrot.androidsdk.AdChoices;
import com.avocarrot.androidsdk.AdError;
import com.avocarrot.androidsdk.AvocarrotCustom;
import com.avocarrot.androidsdk.AvocarrotCustomListener;
import com.avocarrot.androidsdk.CustomModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* Compatible with Avocarrot SDK 3.7.1+ */

public class AvocarrotNativeMopub extends CustomEventNative {

    private static final String PLACEMENT_KEY = "placementKey";
    private static final String API_KEY = "apiKey";

    private static final String SANDBOX = "sandbox";
    private static final String LOGGER = "logger";

    AvocarrotCustom mAvocarrotCustom;

    @Override
    protected void loadNativeAd(
            final @NonNull Context context,
            final @NonNull CustomEventNativeListener customEventNativeListener,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras
    ) {

        final String placement;
        final String appId;
        if (extrasAreValid(serverExtras)) {
            placement = serverExtras.get(PLACEMENT_KEY);
            appId = serverExtras.get(API_KEY);
        } else {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mAvocarrotCustom = new AvocarrotCustom(unwrapActivity(context), appId, placement, "mopub");

        boolean sandbox = false;
        try {
            if (serverExtras.containsKey(SANDBOX)) {
                sandbox = Boolean.parseBoolean(serverExtras.get(SANDBOX));
            }
        } catch (Exception e) {
            sandbox = false;
        }
        boolean logger = false;
        try {
            if (serverExtras.containsKey(LOGGER)) {
                logger = Boolean.parseBoolean(serverExtras.get(LOGGER));
            }
        } catch (Exception e) {
            logger = false;
        }

        mAvocarrotCustom.setSandbox(sandbox);
        mAvocarrotCustom.setLogger(logger, "ALL");

        mAvocarrotCustom.setListener(new AvocarrotCustomListener() {

            AvocarrotNativeAd avocarrotNativeAd = null;

            @Override
            public void onAdLoaded(List<CustomModel> ads) {
                super.onAdLoaded(ads);
                if ((ads != null) && (ads.size() > 0)) {
                    CustomModel model = ads.get(0);
                    if (model != null) {
                        avocarrotNativeAd = new AvocarrotNativeAd(model, mAvocarrotCustom, context, customEventNativeListener);
                    } else {
                        customEventNativeListener.onNativeAdFailed(NativeErrorCode.EMPTY_AD_RESPONSE);
                    }
                } else {
                    customEventNativeListener.onNativeAdFailed(NativeErrorCode.EMPTY_AD_RESPONSE);
                }
            }

            @Override
            public void onAdError(AdError error) {
                super.onAdError(error);
                customEventNativeListener.onNativeAdFailed(NativeErrorCode.EMPTY_AD_RESPONSE);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (avocarrotNativeAd!=null)
                    avocarrotNativeAd.notifyAdImpressed();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (avocarrotNativeAd!=null)
                    avocarrotNativeAd.notifyAdClicked();
            }

        });

        mAvocarrotCustom.loadAd();

    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return (serverExtras!=null) && serverExtras.containsKey(PLACEMENT_KEY) && (serverExtras.containsKey(API_KEY));
    }

    private Activity unwrapActivity(Context context) {
        while (!(context instanceof Activity) && (context instanceof ContextWrapper)) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        return (context instanceof Activity) ? (Activity) context : null;
    }

    class AvocarrotNativeAd extends StaticNativeAd implements View.OnClickListener {

        final CustomModel avocarrotModel;
        final AvocarrotCustom avocarrotCustom;

        public AvocarrotNativeAd(@NonNull CustomModel customModel, AvocarrotCustom avocarrotCustom, final Context context, final CustomEventNativeListener customEventNativeListener) {

            this.avocarrotModel = customModel;
            this.avocarrotCustom = avocarrotCustom;

            setIconImageUrl(avocarrotModel.getIconUrl());
            setMainImageUrl(avocarrotModel.getImageUrl());

            setTitle(avocarrotModel.getTitle());
            setCallToAction(avocarrotModel.getCTAText());
            setText(avocarrotModel.getDescription());

            setClickDestinationUrl(avocarrotModel.getDestinationUrl());

            AdChoices adChoices = avocarrotModel.getAdChoices();
            if (adChoices!=null) {
                setPrivacyInformationIconClickThroughUrl(adChoices.getRedirectionUrl());
                setPrivacyInformationIconImageUrl(adChoices.getIconUrl());
            }

            final List<String> imageUrls = new ArrayList<String>();
            final String mainImageUrl = getMainImageUrl();
            if (mainImageUrl != null) {
                imageUrls.add(getMainImageUrl());
            }
            final String iconUrl = getIconImageUrl();
            if (iconUrl != null) {
                imageUrls.add(getIconImageUrl());
            }

            NativeImageHelper.preCacheImages(context, imageUrls, new NativeImageHelper.ImageListener() {
                @Override
                public void onImagesCached() {
                    customEventNativeListener.onNativeAdLoaded(AvocarrotNativeAd.this);
                }

                @Override
                public void onImagesFailedToCache(NativeErrorCode errorCode) {
                    customEventNativeListener.onNativeAdFailed(errorCode);
                }
            });


        }

        @Override
        public void prepare(final View view) {
            super.prepare(view);
            avocarrotCustom.bindView(avocarrotModel, view, null);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            avocarrotCustom.handleClick(avocarrotModel);
        }

    }

}
