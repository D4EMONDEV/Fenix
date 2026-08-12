/**
 * One configured highlight.js, shared by the documentation and the generator.
 *
 * Both used to import `highlight.js/lib/core` directly, and only the
 * documentation registered any languages — so the generator's preview was
 * highlighted or not depending on whether the documentation module happened to
 * have been evaluated first. It worked, for a reason nothing stated.
 */
import hljs from 'highlight.js/lib/core';
import java from 'highlight.js/lib/languages/java';
import json from 'highlight.js/lib/languages/json';
import bash from 'highlight.js/lib/languages/bash';
import kotlin from 'highlight.js/lib/languages/kotlin';
import xml from 'highlight.js/lib/languages/xml';
import groovy from 'highlight.js/lib/languages/groovy';
import properties from 'highlight.js/lib/languages/properties';

// Registered one by one rather than pulling in the full language pack: the
// whole of highlight.js is larger than everything else on this site put
// together, and Fenix documentation is written in a handful of languages.
// The generator shares this instance, which is why Groovy is here: a project
// generated with Groovy build scripts would otherwise preview unhighlighted.
hljs.registerLanguage('java', java);
hljs.registerLanguage('json', json);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('groovy', groovy);
hljs.registerLanguage('properties', properties);

export default hljs;
