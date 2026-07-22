import logo from '../assets/logo.svg';

/**
 * The Fenix mark: a flame drawn on a 16×16 grid, one square per pixel.
 *
 * Rendered as an image rather than inlined, so the browser scales it with
 * `image-rendering: pixelated` and the squares stay square at every size. An
 * inlined SVG would be resampled and the mark would go soft.
 */
export function Logo({ size = 32 }: { size?: number }) {
  return <img className="logo" src={logo} width={size} height={size} alt="" aria-hidden="true" />;
}
