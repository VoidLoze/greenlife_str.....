package kz.greenleaf.store.site;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kz.greenleaf.store.common.ApiException;
import kz.greenleaf.store.config.AppProperties;
import kz.greenleaf.store.config.CacheConfig;
import kz.greenleaf.store.media.MediaProcessor;
import kz.greenleaf.store.media.MediaUrls;
import kz.greenleaf.store.media.StorageTransactions;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Storefront settings that admins edit at runtime; unset values fall back to application properties. */
@Service
public class SiteSettingsService {

    static final String VIDEO_URL = "company_video_url";
    static final String VIDEO_TITLE = "company_video_title";
    static final String VIDEO_FILE_KEY = "company_video_file_key";
    static final String PROMO_ENABLED = "promo_banner_enabled";
    static final String PROMO_TEXT = "promo_banner_text";
    static final String PROMO_UNTIL = "promo_banner_until";
    static final String DEFAULT_PROMO_TEXT = "Подарок для первых клиентов: при оформлении любого заказа мы вложим в вашу "
            + "посылку приятный и эстетичный комплимент для домашнего уюта от нашего бренда.";

    private final JdbcTemplate jdbc;
    private final AppProperties properties;
    private final MediaProcessor processor;
    private final MediaUrls urls;
    private final StorageTransactions storageTx;

    public SiteSettingsService(JdbcTemplate jdbc, AppProperties properties, MediaProcessor processor, MediaUrls urls,
                               StorageTransactions storageTx) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.processor = processor;
        this.urls = urls;
        this.storageTx = storageTx;
    }

    public record CompanyVideo(String url, String title, boolean uploaded, boolean custom) implements Serializable {
    }

    /** Announcement bar above the storefront; {@code until} is the last day it shows (Almaty time), null — no end. */
    public record PromoBanner(boolean enabled, String text, LocalDate until) implements Serializable {

        public boolean shownOn(LocalDate today) {
            return enabled && text != null && !text.isBlank() && (until == null || !today.isAfter(until));
        }
    }

    @Cacheable(cacheNames = CacheConfig.SITE_SETTINGS, key = "'promo-banner'")
    @Transactional(readOnly = true)
    public PromoBanner promoBanner() {
        Map<String, String> values = load();
        String until = values.get(PROMO_UNTIL);
        return new PromoBanner("true".equals(values.get(PROMO_ENABLED)),
                values.getOrDefault(PROMO_TEXT, DEFAULT_PROMO_TEXT),
                until == null ? null : LocalDate.parse(until));
    }

    @CacheEvict(cacheNames = CacheConfig.SITE_SETTINGS, allEntries = true)
    @Transactional
    public PromoBanner setPromoBanner(boolean enabled, String text, LocalDate until) {
        String trimmed = blankToNull(text);
        if (enabled && trimmed == null) {
            throw ApiException.badRequest("promo_text_required", "Напишите текст баннера");
        }
        put(PROMO_ENABLED, Boolean.toString(enabled));
        put(PROMO_TEXT, trimmed);
        put(PROMO_UNTIL, until == null ? null : until.toString());
        // The cache is cleared after the commit, so reading it back right away could still return the old banner.
        return new PromoBanner(enabled, trimmed == null ? DEFAULT_PROMO_TEXT : trimmed, until);
    }

    @Cacheable(cacheNames = CacheConfig.SITE_SETTINGS, key = "'company-video'")
    @Transactional(readOnly = true)
    public CompanyVideo companyVideo() {
        Map<String, String> values = load();
        String fileKey = values.get(VIDEO_FILE_KEY);
        String url = fileKey != null ? urls.url(fileKey) : values.get(VIDEO_URL);
        String title = values.get(VIDEO_TITLE);
        boolean custom = url != null;
        if (!custom) {
            url = blankToNull(properties.store().companyVideoUrl());
            if (title == null) {
                title = blankToNull(properties.store().companyVideoTitle());
            }
        }
        return new CompanyVideo(url, title, fileKey != null, custom);
    }

    @CacheEvict(cacheNames = CacheConfig.SITE_SETTINGS, allEntries = true)
    @Transactional
    public void setCompanyVideoLink(String url, String title) {
        String link = blankToNull(url);
        if (link != null && !link.matches("^https?://\\S+$")) {
            throw ApiException.badRequest("invalid_video_url", "Ссылка на видео должна начинаться с http:// или https://");
        }
        // Saving only a new caption keeps the uploaded file; it is replaced only by an actual link
        // (or removed with "вернуть видео по умолчанию").
        if (link != null) {
            dropUploadedFile();
            put(VIDEO_URL, link);
        } else if (load().get(VIDEO_FILE_KEY) == null) {
            put(VIDEO_URL, null);
        }
        put(VIDEO_TITLE, blankToNull(title));
    }

    @CacheEvict(cacheNames = CacheConfig.SITE_SETTINGS, allEntries = true)
    @Transactional
    public void uploadCompanyVideo(MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("empty_file", "Файл не выбран или пустой");
        }
        String key;
        try {
            key = processor.storeVideo("site", file, file.getSize(), file.getOriginalFilename()).storageKey();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store company video", ex);
        }
        storageTx.deleteOnRollback(key);
        dropUploadedFile();
        put(VIDEO_FILE_KEY, key);
        put(VIDEO_URL, null);
        if (title != null) {
            put(VIDEO_TITLE, blankToNull(title));
        }
    }

    @CacheEvict(cacheNames = CacheConfig.SITE_SETTINGS, allEntries = true)
    @Transactional
    public void resetCompanyVideo() {
        dropUploadedFile();
        put(VIDEO_URL, null);
        put(VIDEO_TITLE, null);
    }

    private void dropUploadedFile() {
        String previous = load().get(VIDEO_FILE_KEY);
        if (previous != null) {
            put(VIDEO_FILE_KEY, null);
            storageTx.deleteAfterCommit(List.of(previous));
        }
    }

    private Map<String, String> load() {
        Map<String, String> values = new HashMap<>();
        jdbc.query("select key, value from site_settings where value is not null",
                rs -> {
                    values.put(rs.getString(1), rs.getString(2));
                });
        return values;
    }

    private void put(String key, String value) {
        if (value == null) {
            jdbc.update("delete from site_settings where key = ?", key);
            return;
        }
        jdbc.update("""
                insert into site_settings (key, value, updated_at) values (?, ?, now())
                on conflict (key) do update set value = excluded.value, updated_at = now()
                """, key, value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
