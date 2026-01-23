# Guide to writing emails for Tolgee
This is a resource helpful for people contributing to Tolgee who might face the need to create new emails: this
document is a quick summary of how to write emails using React Email and get familiar with the internal tools and the
expected way emails should be written.

## React Email basics
### Why React Email
The use of [React Email](https://react.email/) allows quickly writing emails using clear JSX syntax, which gets turned
into HTML code tailored specifically for compatibility with email clients. This sounds like nothing, but open up one
of the output HTML files, and you'll see for yourself why it's such a big deal to have a tool do it for you. ;)

React Email exposes a handful of primitives documented on their [website](https://react.email/docs/introduction).
If you need real world examples, they provide a bunch of great examples based on real-world emails written using
React Email [here](https://demo.react.email/preview/newsletters/stack-overflow-tips).

They also provide a handful of components [here](https://react.email/components)

### Preview and build
While working on emails, you can use `npm run dev` to spin up a dev server and have a live preview of the emails in
your browser. This allows for convenient workflow without having to send the emails to yourself just to test.

You'll see below how to deal with variables, and how to have test data to see how it looks still without resorting
to manual testing within Tolgee itself.

To build emails, simply run `npm run build`. This will output [Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)
templates in the `out` folder that the backend will be able to consume and render.

The resources used by emails stored in `resources` must be served by the backend at `/static/emails`. Filenames must
be preserved.

> [!NOTE]
> The backend build includes the necessary Gradle tasks to build the emails by itself. You should not need to worry
> about building them yourself or copy files around.

### TailwindCSS
For styles, React Email has a great [TailwindCSS](https://tailwindcss.com/) integration that gets turned into
email-friendly inline styles.

When using styles, make sure to use things that are "email friendly". That means, no flexbox, no grid, and pretty much
anything that's cool in \[CURRENT_YEAR]. [Can I Email](https://www.caniemail.com/) is a good resource for what is
fine to send and what isn't; basically the [Can I Use](https://caniuse.com/) of emails.

This also applies to the layout; always prefer React Email's `Container`, `Row` and `Column` elements for layout.
They'll get turned into ugly HTML tables to do the layout - just like in the good ol' HTML days...

> [!TIP]
> Recent versions of React Email have an embedded linter in preview mode, that checks for compatibility and other
> helpful things.

## Layouts
The core shell of emails is provided by `components/layouts/LayoutCore.tsx`. It is not expected to be used as-is, but
instead to serve as a shared base for more complete layouts such as `ClassicLayout.tsx`. All emails should use a layout,
or at least must use the core layout as it contains important building blocks for emails to work properly.

The classic layout (`ClassicLayout.tsx`) takes 3 properties:
- `subject` (required): displayed in the header of the email and be used to construct the actual email subject
- `sendReason` (required): important for anti-spam laws and must reflect the reason why a given email is sent
  - Is it because they have an account? Is it because they enabled notifications? ...
- `extra` (optional): displayed at the very bottom, useful to insert an unsubscribe link if necessary

These three properties are generally expected to receive output from the `t.raw()` function documented below. The core
layout only requires the subject.

## Utility components
This is note is left here for the lack of a better section: whenever you need a dynamic properties (e.g. href that
takes the value of a variable), you can prefix your attribute with `data-th-` and setting the value to a
[Thymeleaf Standard Expression](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#standard-expression-syntax).

```jsx
<a data-th-href="${link}">Click here!</a>
```

A few low-level base components with default styles are available in `components/atoms`, such as buttons.
Shared parts are found in `components/parts`.

### `<LocalizedText />` and `t()`
Most if not all text in emails are expected to be wrapped in `<LocalizedText />` (or `t()` when more appropriate).
They are equivalent : `<LocalizedText />` is simply a JSX wrapper for calling `t()`.

There is also `t.raw()`, which works exactly like `t()` but enforces the translation to be plaintext (no HTML). It's
mainly used for the subject part and the send reason.

The strings are written using a format similar to the familiar Tolgee ICU, via [ICU4J](https://github.com/unicode-org/icu/tree/main/icu4j)
(see [MessageFormat](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/MessageFormat.html)). A simple but working post-processing step by the custom message source adds support for
FormatJS-like XML within the message.

> [!TIP]
> `b`, `i`, `u`, `em`, and `strong` tags are pre-configured and will work out-of-the-box without special config.

ICU arguments are pulled from the template variables. To access a dotted key, such as `item.name` use a double
underscore: `item__name`.

The `<LocalizedText />` takes the following properties:
- `keyName` (required): String key name
- `defaultValue` (required): Default string to use
  - Will be used by the CLI to push default values when pushing new keys, and during preview
  - Used to detect which XML tags and variables the message expects, to build the Thymeleaf template accordingly
- `demoParams` (required¹): Demo properties to use when rendering the string
  - When previewing, the ICU string will be rendered using these values, so it is representative of a "real" email
  - If demo props and tag renderers are not specified, the email will fail to render both in preview and at build-time
  - ¹It can be unset if there are no props in the string

The `t()` function (and `t.raw()`) takes the same properties, taking them as arguments in the order they're described
here.

> [!WARNING]
> When using the development environment, only the default value locally provided will be considered. Strings are not
> pulled from Tolgee to test directly within the dev environment. (At least, at this time).

```tsx
<LocalizedText
  keyName="hello"
  defaultValue="Hello {name}!"
  demoParams={{
    name: 'Bob'
  }}
/>

t('hello', 'Hello {name}!', { name: 'Bob' })
```

#### Considerations for the renderer
Newlines are handled as if there was an explicit `<br />`. This is handled by the previewer, and must be correctly
handled by the renderer (by replacing all newlines from ICU format output by `<br />`).

HTML injection attacks are prevented by Thymeleaf and the template pre-processing, by making sure variables are always
escaped by default (via `Var` or an ICU string).

### `<Var />`
Injects a variable as plaintext. Easy, simple. Only useful when a variable is used outside an ICU string.

It takes the following arguments:
- `variable` (required): name of the variable
- `demoValue` (required): value used for the preview
- `dangerouslyInjectValueAsHtmlWithoutSanitization` (optional): whether to inject this variable as raw HTML. **VERY DANGEROUS. WILL LEAD TO <abbr title="Cross-Site Scripting">XSS</abbr> ATTACKS IF MISUSED.** Defaults to `false`

### `<ImgResource />`
If you want to use images, images should be placed in the `resources` folder and then this component should be used.
It functions like [React Email's `<Img />`](https://react.email/docs/components/image), except it doesn't take a
`src` prop but a `resource`, that should be the name of the file you want to insert.

Be careful, [**SVG images are poorly supported**](https://www.caniemail.com/features/image-svg/) and therefore should
be avoided. PNG, JPG, and GIF should be good.

It is also very important that files are **never** deleted, and preferably not modified. Doing so would alter
previously sent emails, by modifying images shown when opening them or leading to broken images.

### `<If />`
This allows for a conditionally showing a part of the email (and optionally showing something else instead).
This component takes exactly one or two children: the `true` case and the `false` case. You should always use
`<If.Then />` and `<If.Else />` for the sake of clarity.

It receives the following properties:
- `condition` (required): the [Thymeleaf conditional expression](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#simple-conditionals-if-and-unless)
- `demoValue` (optional): the demo value. Defaults to `true`

### `<For />`
When dealing with a list of items, this component allows iterating over each element of the array and produce the
inner HTML for each element of the array.

The component must have exactly one child; to render multiple nodes make sure to use a fragment.

This component receives the following properties:
- `each` (required): The [Thymeleaf iterator expression](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#using-theach)
- `demoIterations` (required): Number of times the children should be rendered in development preview

## Global variables
The following global variables are available:
- `isCloud` (boolean): Whether this is Tolgee Cloud or not
- `instanceQualifier`: Either "Tolgee" for Tolgee Cloud, or the domain name used for the instance
- `backendUrl`: Base URL of the backend

They still need to be passed as demo values, except for localized strings as a default value is provided then.
The default value can be overridden.

## Tips & tricks
How the social icons were generated:
- Get SVG from https://simpleicons.org/
- Write to `[file].svg`
- Add `width="16" height="16" fill="#a0a0a0"` to the `<svg>` tag
- Convert SVG to PNG
- Drink a deserved coffee
