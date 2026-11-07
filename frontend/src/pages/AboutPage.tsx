import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { catalogApi } from '@/api/endpoints'
import { ABOUT } from '@/content/about'
import { GiftIcon, TelegramIcon, WhatsappIcon } from '@/components/Icons'
import { useT } from '@/i18n'
import { money, whatsappLink } from '@/lib/format'
import { useDocumentTitle, useSettings } from '@/lib/hooks'

export default function AboutPage() {
  const { t, lang } = useT()
  const copy = ABOUT[lang === 'kk' ? 'kk' : 'ru']
  const { data: settings } = useSettings()
  const { data: home } = useQuery({ queryKey: ['home'], queryFn: catalogApi.home })
  useDocumentTitle(copy.eyebrow)

  const discount = settings?.partnerDiscountPercent || 50
  const threshold = settings?.freeDeliveryThreshold || 7000
  const [deliveryTitle, deliveryText] = copy.facts.delivery(money(threshold))
  const [cardTitle, cardText] = copy.facts.card(discount)

  return (
    <article className="about">
      <section className="about-hero">
        <div className="about-hero-text">
          <span className="eyebrow">{copy.eyebrow}</span>
          <h1>{copy.title}</h1>
          {copy.lead.map((line) => (
            <p key={line} className="about-lead">
              {line}
            </p>
          ))}
          <div className="about-actions">
            <Link to="/catalog" className="btn btn-gold btn-lg">
              {t.viewCatalog} →
            </Link>
            <Link to="/partner" className="btn btn-ghost btn-lg">
              {t.partner}
            </Link>
          </div>
        </div>
        <figure className="about-hero-photo">
          <img src="/img/gifts/gift-set-2.jpg" alt="Greenleaf family — Born for Beauty" width={1024} height={676} />
        </figure>
      </section>

      <section className="about-quote about-reveal">
        <span className="about-quote-mark" aria-hidden="true">
          “
        </span>
        <blockquote>{copy.quote}</blockquote>
        <p>{copy.philosophy}</p>
      </section>

      <section className="about-block">
        <h2 className="about-title">{copy.valuesTitle}</h2>
        <div className="about-values">
          {copy.values.map((value) => (
            <article key={value.title} className="about-value about-reveal">
              <span className="about-value-icon" aria-hidden="true">
                {value.icon}
              </span>
              <h3>{value.title}</h3>
              <p>{value.text}</p>
            </article>
          ))}
        </div>
      </section>

      {settings?.promoBanner && (
        <section className="about-opening about-reveal">
          <span className="about-opening-icon" aria-hidden="true">
            <GiftIcon size={28} />
          </span>
          <div>
            <span className="eyebrow">{copy.openingEyebrow}</span>
            <h2>{copy.openingTitle}</h2>
            <p className="about-opening-text">{copy.openingText}</p>
            <p className="about-opening-gift">{settings.promoBanner}</p>
          </div>
        </section>
      )}

      <section className="about-block">
        <h2 className="about-title">{copy.factsTitle}</h2>
        <ul className="about-facts">
          <li className="about-fact about-reveal">
            <span className="about-fact-icon" aria-hidden="true">
              ★
            </span>
            <strong>{copy.facts.first}</strong>
          </li>
          {home && home.productCount > 0 && (
            <li className="about-reveal">
              <Link to="/catalog" className="about-fact">
                <span className="about-fact-icon" aria-hidden="true">
                  🌿
                </span>
                <strong>{copy.facts.catalog(home.productCount, home.categories.length)}</strong>
                <span className="about-fact-more">{t.viewCatalog} →</span>
              </Link>
            </li>
          )}
          <li className="about-fact about-reveal">
            <span className="about-fact-icon" aria-hidden="true">
              🚚
            </span>
            <strong>{deliveryTitle}</strong>
            <span>{deliveryText}</span>
          </li>
          <li className="about-reveal">
            <Link to="/partner" className="about-fact">
              <span className="about-fact-icon" aria-hidden="true">
                %
              </span>
              <strong>{cardTitle}</strong>
              <span>{cardText}</span>
              <span className="about-fact-more">{t.morePartner} →</span>
            </Link>
          </li>
        </ul>
      </section>

      <section className="about-closing about-reveal">
        <img className="about-closing-logo" src="/logo-full-sm.webp" alt="Greenleaf family" loading="lazy" />
        <p className="about-closing-text">{copy.closing}</p>
        <p className="about-closing-call">{copy.closingCall}</p>
        <div className="about-actions">
          <Link to="/catalog" className="btn btn-gold btn-lg">
            {t.viewCatalog} →
          </Link>
          {settings?.whatsapp && (
            <a className="btn btn-whatsapp btn-lg" href={whatsappLink(settings.whatsapp, 'Здравствуйте!')} target="_blank" rel="noreferrer">
              <WhatsappIcon size={18} /> WhatsApp
            </a>
          )}
          {settings?.instagramUrl && (
            <a className="btn btn-ghost btn-lg" href={settings.instagramUrl} target="_blank" rel="noreferrer">
              Instagram
            </a>
          )}
          {settings?.telegramUrl && (
            <a className="btn btn-ghost btn-lg" href={settings.telegramUrl} target="_blank" rel="noreferrer">
              <TelegramIcon size={18} /> Telegram
            </a>
          )}
        </div>
        <strong className="about-welcome">{copy.welcome}</strong>
      </section>
    </article>
  )
}
