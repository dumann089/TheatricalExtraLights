#version 150

// Panache CO2 : raymarch d'un cone de brouillard dense et blanc, ombre de gris sur son flanc non
// eclaire, front dechiquete par le bruit, coupure de vanne qui remonte le jet. Rendu premultiplie
// (ONE, ONE_MINUS_SRC_ALPHA) : la fumee couvre ce qui est derriere elle, ce n'est pas de la lumiere.

uniform sampler2D Sampler1;   // profondeur de la scene

uniform mat4 InvProjMat;
uniform vec2 ScreenSize;
uniform int StepCount;
uniform float Time;

uniform vec3 Nozzle;        // bouche de la buse, espace vue
uniform vec3 Axis;          // axe du jet, espace vue, unitaire
uniform vec3 SideU;         // base fixe dans le monde, perpendiculaire a l'axe, espace vue
uniform vec3 SideV;
uniform vec3 UpV;           // verticale du monde en espace vue
uniform float PlumeLength;  // distance du front depuis la buse, blocs
uniform float CutFront;     // front de coupure depuis la buse, blocs ; negatif = vanne ouverte
uniform float Pressure;     // 0..1, intensite DMX
uniform float Dissipate;    // 0 vanne ouverte .. 1 nuage dissipe : le gaz lache s'etale et se dilue
uniform float FlowClock;    // horloge du gaz, ticks : ralentit une fois la vanne fermee
uniform float ExitSpeed;    // vitesse de sortie du gaz, blocs/tick
uniform float BaseFlow;     // debit courant a la buse / pression du panache : un fader qui descend amincit la base
uniform float NozzleRadius; // rayon a la bouche
uniform float FlashRadius;  // rayon apres la detente eclair
uniform float ConeTan;      // tangente du demi-angle du cone
uniform float Brightness;

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i);
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));
    return mix(
        mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
        mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y),
        f.z
    );
}

// Trois octaves : volutes larges et grain fin par-dessus.
float fbm(vec3 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 3; i++) {
        sum += amp * vnoise(p);
        p = p * 2.17 + vec3(11.3, 7.1, 5.9);
        amp *= 0.5;
    }
    return sum;
}

float ign(vec2 px) {
    return fract(52.9829189 * fract(0.06711056 * px.x + 0.00583715 * px.y));
}

vec3 reconstructViewPos(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InvProjMat * clip;
    return view.xyz / max(view.w, 1.0e-6);
}

const float DRAG = 0.905;
const float LN_DRAG = -0.09983;   // ln(0.905)

// Temps de vol du gaz jusqu'a z. Avec le freinage par tick, v(z) = v0 (1 - z / Linf) : rapide a la
// buse, presque nul a la tete. Plancher a 0.2 v0 pour ne pas tasser le bruit a l'infini pres du front.
float travelTime(float z) {
    float linf = ExitSpeed / (1.0 - DRAG);
    float zc = 0.8 * linf;
    float z1 = clamp(z, 0.0, zc);
    float t = log(1.0 - z1 / linf) / LN_DRAG;
    return t + max(z - zc, 0.0) / (0.2 * ExitSpeed);
}

// Rayon du panache a la distance z : detente eclair sur le premier demi-bloc, cone, tete qui gonfle.
float plumeRadius(float z) {
    float flash = NozzleRadius + (FlashRadius - NozzleRadius) * smoothstep(0.0, 0.6, z);
    float head = 1.0 + 0.7 * smoothstep(PlumeLength - 2.5, PlumeLength, z);
    return (flash + z * ConeTan) * head;
}

// Cone fini autour de l'axe, rayon rBase a la buse et rBase + slope * z ensuite, de zMin a zMax :
// borne de la marche, serree sur le panache pour ne pas marcher dans le vide.
bool intersectPlumeBound(vec3 ro, vec3 rd, float rBase, float slope, float zMin, float zMax, out float t0, out float t1) {
    vec3 o = ro - Nozzle;
    float oz = dot(o, Axis);
    float dz = dot(rd, Axis);
    float ta;
    float tb;
    if (abs(dz) < 1.0e-6) {
        if (oz < zMin || oz > zMax) return false;
        ta = 0.0;
        tb = 1.0e9;
    } else {
        ta = (zMin - oz) / dz;
        tb = (zMax - oz) / dz;
        if (ta > tb) {
            float tmp = ta;
            ta = tb;
            tb = tmp;
        }
        ta = max(ta, 0.0);
        if (tb <= ta) return false;
    }
    vec3 op = o - Axis * oz;
    vec3 dp = rd - Axis * dz;
    float rz = rBase + slope * oz;
    float a = dot(dp, dp) - slope * slope * dz * dz;
    float b = 2.0 * (dot(op, dp) - slope * dz * rz);
    float c = dot(op, op) - rz * rz;
    if (abs(a) < 1.0e-6) {
        if (abs(b) < 1.0e-9) {
            if (c > 0.0) return false;
            t0 = ta;
            t1 = tb;
            return true;
        }
        float tr = -c / b;
        if (b > 0.0) {
            t0 = ta;
            t1 = min(tb, tr);
        } else {
            t0 = max(ta, tr);
            t1 = tb;
        }
        return t1 > t0;
    }
    if (a > 0.0) {
        float disc = b * b - 4.0 * a * c;
        if (disc < 0.0) return false;
        float sq = sqrt(disc);
        t0 = max(ta, (-b - sq) / (2.0 * a));
        t1 = min(tb, (-b + sq) / (2.0 * a));
        return t1 > t0;
    }
    // Rayon plus incline que la surface du cone (vue dans l'axe) : l'interieur est hors des racines,
    // on garde toute la tranche, large mais sure.
    t0 = ta;
    t1 = tb;
    return true;
}

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float sceneT = length(reconstructViewPos(uv, texture(Sampler1, uv).r));
    vec3 rd = normalize(reconstructViewPos(uv, 1.0));

    float len = max(PlumeLength, 0.05);
    // Nuage lache : il s'etale (rayon), se dilue (densite) et s'effiloche (bord). Courbe en S pour
    // l'etalement et le bord, densite qui tend vers zero sans cassure : rien ne saute a la fin.
    float k = Dissipate * Dissipate * (3.0 - 2.0 * Dissipate);
    float thin = pow(1.0 - Dissipate, 1.6);
    float spread = 1.0 + 1.2 * k;
    if (thin < 0.002) {
        discard;
    }
    // Borne : cone de la detente eclair a la tete gonflee, avec la marge du bord erode (rho < 1.6).
    float zMax = len + 1.0;
    float rBase = FlashRadius * spread * 1.6 + 0.1;
    float rEnd = plumeRadius(len) * spread * 1.6 + 0.1;
    float slope = max(rEnd - rBase, 0.0) / zMax;
    float t0;
    float t1;
    if (!intersectPlumeBound(vec3(0.0), rd, rBase, slope, -0.15, zMax, t0, t1)) {
        discard;
    }
    t1 = min(t1, sceneT - 0.02);
    if (t1 <= t0) {
        discard;
    }

    // Pas fixe d'environ 0.2 bloc, le dernier tronque : les echantillons ne glissent pas quand le
    // nombre de pas change d'une image a l'autre. Au-dela du plafond de qualite, le pas s'allonge.
    float marchLen = t1 - t0;
    int maxSteps = clamp(StepCount, 16, 48);
    float dt = max(0.2, marchLen / float(maxSteps));
    int steps = min(maxSteps, int(ceil(marchLen / dt)));
    float t = t0 + dt * ign(gl_FragCoord.xy);

    // Lumiere de scene : depuis la camera, relevee vers le haut.
    vec3 lightDir = normalize(-rd + UpV * 0.8);
    float sigma = 9.0 * (0.6 + 0.4 * Pressure) * thin;
    vec3 tint = vec3(0.97, 0.985, 1.0) * Brightness;

    vec3 accum = vec3(0.0);
    float alpha = 0.0;
    for (int i = 0; i < 48; i++) {
        if (i >= steps || t > t1 || alpha > 0.985) {
            break;
        }
        vec3 p = rd * t;
        vec3 o = p - Nozzle;
        float z = dot(o, Axis);
        vec3 perp = o - Axis * z;
        float r = length(perp);
        if (z > -0.1 && z < len + 1.0) {
            float zc = max(z, 0.0);
            float radius = plumeRadius(zc) * spread;
            float rho = r / max(radius, 1.0e-3);
            if (rho < 1.6) {
                // Bruit lagrangien : chaque parcelle de gaz garde sa valeur de bruit (son instant de
                // depart) et avance a la vitesse locale du gaz, vite a la buse, lentement a la tete.
                float label = ExitSpeed * (FlowClock - travelTime(zc));
                vec3 q = vec3(dot(perp, SideU), dot(perp, SideV), label);
                float n = fbm(q * 1.5);
                float n2 = vnoise(q * 5.0 + vec3(3.1, 7.7, 0.0));
                // Bord erode par les volutes.
                float edge = 1.0 - rho + (n - 0.5) * (0.9 + 0.6 * k) + (n2 - 0.5) * 0.3 - 0.5 * k;
                float d = smoothstep(0.0, 0.45, edge);
                // Front dechiquete, dilution vers la tete.
                float front = len + (n - 0.5) * 2.5;
                d *= 1.0 - smoothstep(front - 1.8, front + 0.3, z);
                d *= mix(1.0, 0.35, smoothstep(0.45 * len, len, z));
                // Fader en descente : le gaz qui sort maintenant est plus maigre, le gaz deja parti non.
                d *= mix(BaseFlow, 1.0, smoothstep(0.0, 2.5, zc));
                // Coupure de vanne : derriere le front de coupure il n'y a plus de gaz. Bande large
                // et brouillee par le bruit, pour une limite qui s'estompe au lieu de balayer.
                if (CutFront > -10.0) {
                    d *= smoothstep(CutFront - 1.2, CutFront + 2.2, z + (n - 0.5) * 1.8);
                }
                if (d > 0.001) {
                    vec3 nrm = r > 1.0e-4 ? perp / r : vec3(0.0);
                    float lit = 0.5 + 0.5 * dot(nrm, lightDir);
                    float shade = (0.55 + 0.45 * lit) * (0.9 + 0.1 * (1.0 - rho)) * (0.88 + 0.35 * (n - 0.5));
                    float a = 1.0 - exp(-sigma * d * dt);
                    float w = a * (1.0 - alpha);
                    accum += tint * shade * w;
                    alpha += w;
                }
            }
        }
        t += dt;
    }
    if (alpha < 0.003) {
        discard;
    }
    fragColor = vec4(accum, alpha);
}
