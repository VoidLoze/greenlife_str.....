package kz.greenleaf.store.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import kz.greenleaf.store.site.SiteSettingsService;
import kz.greenleaf.store.site.SiteSettingsService.CompanyVideo;
import kz.greenleaf.store.site.SiteSettingsService.PromoBanner;
import kz.greenleaf.store.site.StoreSettingsService;
import kz.greenleaf.store.site.StoreSettingsService.StoreSettings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Settings")
public class PublicSettingsController {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Almaty");

    private final StoreSettingsService storeSettings;
    private final SiteSettingsService siteSettings;
    private final Clock clock;

    public PublicSettingsController(StoreSettingsService storeSettings, SiteSettingsService siteSettings, Clock clock) {
        this.storeSettings = storeSettings;
        this.siteSettings = siteSettings;
        this.clock = clock;
    }

    @GetMapping("/api/settings/public")
    @Operation(summary = "Контакты магазина, условия доставки, видео о компании и баннер акции для витрины")
    public PublicSettings settings() {
        StoreSettings s = storeSettings.current();
        CompanyVideo video = siteSettings.companyVideo();
        PromoBanner promo = siteSettings.promoBanner();
        String promoText = promo.shownOn(LocalDate.now(clock.withZone(STORE_ZONE))) ? promo.text() : null;
        return new PublicSettings(s.storeName(), s.phone(), s.whatsapp(), s.telegramUrl(), s.instagramUrl(), s.city(),
                s.address(), s.workingHours(), s.deliveryFee(), s.freeDeliveryThreshold(), s.freeDeliveryRetailOnly(),
                s.partnerDiscountPercent(), s.maxQuantityPerItem(), video.url(), video.title(), promoText);
    }

    public record PublicSettings(String storeName, String phone, String whatsapp, String telegramUrl,
                                 String instagramUrl, String city, String address, String workingHours,
                                 BigDecimal deliveryFee, BigDecimal freeDeliveryThreshold,
                                 boolean freeDeliveryRetailOnly,
                                 int partnerDiscountPercent, int maxQuantityPerItem,
                                 String companyVideoUrl, String companyVideoTitle, String promoBanner) {
    }
}
