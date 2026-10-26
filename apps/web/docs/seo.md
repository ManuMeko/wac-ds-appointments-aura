# SEO por demo

Cada demo declara su SEO en `data/aura.ts` o `data/taller.ts`, dentro de `seo` y de los datos de contacto que ya usa la ficha. El layout, la home, `app/sitemap.ts`, `app/robots.ts`, `app/manifest.ts` y el JSON-LD leen esa config. No hace falta duplicar títulos en las páginas.

## Variables de entorno

| Variable | Dónde | Qué poner |
| --- | --- | --- |
| `NEXT_PUBLIC_SITE_URL` | `.env.local` o el hosting | Origen canónico, sin barra final. Ejemplo: `https://www.cliente.com`. Si falta, el sitio usa `http://localhost:3000`. Entra en canonical, Open Graph, sitemap, robots y JSON-LD. |
| `NEXT_PUBLIC_DEMO` | el mismo sitio | `aura` o `taller`. Elige config, icono, manifest y el tipo de schema. |

Al cambiar una variable `NEXT_PUBLIC_*` hay que reiniciar `next dev`. El proceso de build también las fija.

## Campos de `seo`

Obligatorios, ya rellenos en las demos:

- `siteName`, `defaultTitle`, `titleTemplate` (`%s · Nombre`)
- `description`, `keywords`, `locale` (`es_ES`), `language` (`es`)
- `ogImage`: ruta pública, 1200×630 o mayor. Es la imagen de Open Graph y de la Twitter card, y también `image` del JSON-LD.
- `themeColor`: color de la barra del navegador y `theme_color` del manifest.
- `backgroundColor`: fondo del manifest. Si se omite, se usa `themeColor`.
- `robots.index` y `robots.follow`.

Opcionales, para el negocio local:

- `schemaType`: `AutoRepair` en un taller. En estética, `["BeautySalon", "HealthAndBeautyBusiness"]`. Sin este campo la home no emite JSON-LD.
- `priceRange`: rango corto (`€`, `€€`, `€€€`). No es un precio inventado de una ficha.
- `areaServed`: ciudad o zona que ya atiende el negocio.
- `geo`: `{ latitude, longitude }` solo si el cliente da las coordenadas (Google Business Profile las muestra). Si no están, no se inventan y el JSON-LD no incluye `geo`.

El JSON-LD de la home usa nombre, teléfono, dirección, horario, `url` e imagen que ya están en `brand`, `contact` y `openingHours`. No añade `AggregateRating`: las reseñas de la demo no son reseñas verificadas.

Horario: el texto `Lunes — Viernes`, `Sábado` y `Domingo`, con horas `8:00 — 18:00`, se convierte en `openingHoursSpecification`. `Cerrado` se omite. Otro formato de días no entra en el schema hasta que se añada al mapa de `lib/jsonld.ts`.

## Iconos

- `public/icons/taller.svg` y `public/icons/aura.svg`: marca estática (inicial y color del tema).
- `app/icon.tsx` y `app/apple-icon.tsx`: PNG generados con la misma marca según `NEXT_PUBLIC_DEMO`.
- Para un cliente, sustituye el SVG y, si hace falta, un `app/favicon.ico` real. No dejes el monograma de la demo en el dominio del cliente.

## Sitemap y robots

`/sitemap.xml` incluye `/`, `/reservar` y los `legalLinks` de la demo (`/aviso-legal`, `/privacidad`, `/cookies`). No incluye `/business`.

`/robots.txt` publica el sitemap. Con `robots.index: false` bloquea todo el sitio. Con `index: true` permite la web pública y bloquea `/business`.

Las demos siguen en `index: false` y `follow: false` porque el negocio es ficticio. En el cliente real:

1. Pon el dominio en `NEXT_PUBLIC_SITE_URL`.
2. Sustituye nombre, dirección, teléfono, horarios, `ogImage` y precios.
3. Pasa `robots.index` y `robots.follow` a `true` solo cuando el dominio de producción esté listo.
4. Redeploy.

## Google Business Profile

Ficha del negocio real, no de la demo. De ahí salen los datos que hay que copiar a la config, sin completar huecos:

- nombre público, categoría (taller mecánico o centro de estética)
- dirección y código postal
- teléfono
- horario, incluidos los días cerrados
- zona de servicio, si aplica
- coordenadas, si se quieren en `seo.geo`
- sitio web: el mismo valor que `NEXT_PUBLIC_SITE_URL`
- fotos: la de portada puede ser `seo.ogImage`

No copies la valoración de Google al JSON-LD como `AggregateRating` salvo que el cliente tenga reseñas propias y se decida mostrarlo aparte. Esta plantilla no lo emite.

## Search Console

1. Verifica la propiedad del dominio o del prefijo `https://www.cliente.com/`.
2. Confirma que `NEXT_PUBLIC_SITE_URL` coincide con la propiedad verificada. Un canonical hacia otro host deja las páginas fuera.
3. Con `robots.index: true` ya desplegado, envía `https://www.cliente.com/sitemap.xml`.
4. Inspecciona `/` y `/reservar`. La home debe tener un solo `h1`, `lang="es"` y un JSON-LD de `AutoRepair` o `BeautySalon` sin errores en la prueba de resultados enriquecidos.
5. Revisa la cobertura unos días después. `/business` no debe entrar: está fuera del sitemap y bloqueado en robots cuando el sitio es indexable.

## Qué no cambia entre demos

Aura no define `icon` en servicios ni en beneficios, así que esas listas no muestran iconos de oficio. El taller sí. El JSON-LD, el manifest y el icono sí cambian con la demo activa, porque cada una tiene su ficha.
