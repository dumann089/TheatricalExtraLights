#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 InvProjMat;
uniform vec3 LightPos;
uniform vec3 LightDir;
uniform vec3 AxisU;
uniform vec3 AxisV;
uniform vec3 OcclusionPos;
uniform vec3 OcclusionNormal;
uniform float TanHalfAngle;
uniform float BaseRadius;
uniform float MaxLen;
uniform float MaxGoboDist;
uniform float GoboRotation;
uniform float WheelTransition;
uniform float Intensity;
uniform float Focus;
uniform vec3 LightColor;
uniform vec2 ScreenSize;
uniform float OcclusionEnabled;

// Module de couteaux (framing shutters) : 4 lames haut / droite / bas / gauche.
uniform float ShutterEnabled;
uniform vec4 BladeA;         // insertion du coin A de chaque lame, 0 (sorti) .. 1 (rentre a fond)
uniform vec4 BladeB;         // insertion du coin B de chaque lame
uniform float FrameRotation; // rotation du module complet, radians

// ── Animation wheel ──────────────────────────────────────────────────────────
// Texture d'effet (flammes, eau, nuages...) qui defile devant la porte, dans le repere
// (u,v) normalise au rayon du faisceau. Une tuile couvre le diametre de la porte.
uniform sampler2D Sampler3;
uniform float AnimEnabled;
uniform float AnimAngle;   // orientation, radians
uniform float AnimOffset;  // defilement, en tuiles

float animationMask(float u, float v, float radius) {
    if (AnimEnabled < 0.5) return 1.0;
    float ca = cos(AnimAngle);
    float sa = sin(AnimAngle);
    vec2 p = vec2(u, v) / max(radius, 0.0001);
    vec2 r = vec2(p.x * ca - p.y * sa, p.x * sa + p.y * ca);
    vec2 uv = fract(vec2(r.x * 0.5 + AnimOffset, r.y * 0.5));
    float a = dot(texture(Sampler3, uv).rgb, vec3(0.299, 0.587, 0.114));
    return clamp(a, 0.0, 1.0);
}

// Repere monde du faisceau, pour retrouver la position monde du point eclaire.
uniform vec3 LightPosW;
uniform vec3 LightDirW;
uniform vec3 AxisUW;
uniform vec3 AxisVW;

// ── Ombres portees ───────────────────────────────────────────────────────────
// Grille d'occupation des blocs (Sampler4 : N tranches empilees, N x N*N, rouge = solide) et
// jusqu'a 8 boites d'entites. Un point est ombre si le segment point→source traverse un
// solide.
uniform sampler2D Sampler4;
uniform float ShadowEnabled;
uniform vec3 VoxelOrigin;   // coin min de la grille, monde
uniform float VoxelCell;    // taille d'une cellule, blocs
uniform float VoxelSize;    // cellules par axe
uniform float OccCount;
uniform vec3 OccMin0;
uniform vec3 OccMax0;
uniform vec3 OccMin1;
uniform vec3 OccMax1;
uniform vec3 OccMin2;
uniform vec3 OccMax2;
uniform vec3 OccMin3;
uniform vec3 OccMax3;
uniform vec3 OccMin4;
uniform vec3 OccMax4;
uniform vec3 OccMin5;
uniform vec3 OccMax5;
uniform vec3 OccMin6;
uniform vec3 OccMax6;
uniform vec3 OccMin7;
uniform vec3 OccMax7;

float voxelSolid(vec3 wp) {
    vec3 c = (wp - VoxelOrigin) / VoxelCell;
    if (c.x < 0.0 || c.y < 0.0 || c.z < 0.0 || c.x >= VoxelSize || c.y >= VoxelSize || c.z >= VoxelSize) return 0.0;
    vec3 i = floor(c);
    vec2 uv = vec2((i.x + 0.5) / VoxelSize, (i.z * VoxelSize + i.y + 0.5) / (VoxelSize * VoxelSize));
    return texture(Sampler4, uv).r;
}

bool segmentHitsBox(vec3 a, vec3 b, vec3 bmin, vec3 bmax) {
    vec3 d = b - a;
    vec3 dd = vec3(
        abs(d.x) < 1.0e-6 ? 1.0e-6 : d.x,
        abs(d.y) < 1.0e-6 ? 1.0e-6 : d.y,
        abs(d.z) < 1.0e-6 ? 1.0e-6 : d.z);
    vec3 t0 = (bmin - a) / dd;
    vec3 t1 = (bmax - a) / dd;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    float tn = max(max(tmin.x, tmin.y), tmin.z);
    float tf = min(min(tmax.x, tmax.y), tmax.z);
    return tf >= max(tn, 0.0) && tn <= 1.0;
}

// startOffset : distance (blocs) a partir du point avant de tester les blocs, pour ne pas
// se faire ombrer par la surface sur laquelle on est.
float shadowFactor(vec3 wp, vec3 lightW, float startOffset) {
    if (ShadowEnabled < 0.5) return 1.0;
    vec3 toL = lightW - wp;
    float len = length(toL);
    if (len < 0.75) return 1.0;
    if (OccCount > 0.5 && segmentHitsBox(wp, lightW, OccMin0, OccMax0)) return 0.0;
    if (OccCount > 1.5 && segmentHitsBox(wp, lightW, OccMin1, OccMax1)) return 0.0;
    if (OccCount > 2.5 && segmentHitsBox(wp, lightW, OccMin2, OccMax2)) return 0.0;
    if (OccCount > 3.5 && segmentHitsBox(wp, lightW, OccMin3, OccMax3)) return 0.0;
    if (OccCount > 4.5 && segmentHitsBox(wp, lightW, OccMin4, OccMax4)) return 0.0;
    if (OccCount > 5.5 && segmentHitsBox(wp, lightW, OccMin5, OccMax5)) return 0.0;
    if (OccCount > 6.5 && segmentHitsBox(wp, lightW, OccMin6, OccMax6)) return 0.0;
    if (OccCount > 7.5 && segmentHitsBox(wp, lightW, OccMin7, OccMax7)) return 0.0;
    vec3 dirL = toL / len;
    float start = min(startOffset, len * 0.5);
    float span = len - start - VoxelCell * 0.75;   // on s'arrete avant la cellule de la source
    if (span <= 0.0) return 1.0;
    int steps = int(clamp(ceil(span / (VoxelCell * 0.9)), 2.0, 32.0));
    float ds = span / float(steps);
    vec3 p = wp + dirL * (start + ds * 0.5);
    for (int i = 0; i < 32; i++) {
        if (i >= steps) break;
        if (voxelSolid(p) > 0.5) return 0.0;
        p += dirL * ds;
    }
    return 1.0;
}

const float GOBO_BRIGHTNESS=0.55;

// Convention A/B (grandMA) : le bord de la lame relie les deux coins A et B.
float bladeMask(vec2 p, vec2 n, float insA, float insB, float soft){
    if(insA<=0.0005&&insB<=0.0005)return 1.0;
    vec2 t=vec2(n.y,-n.x);
    const float R=1.15;
    vec2 cornerA=n*(R-2.0*R*insA)-t*R;
    vec2 cornerB=n*(R-2.0*R*insB)+t*R;
    vec2 edge=cornerB-cornerA;
    vec2 m=normalize(vec2(edge.y,-edge.x));
    if(dot(m,n)<0.0)m=-m;
    float sd=dot(p-cornerA,m);
    return 1.0-smoothstep(-soft,soft,sd);
}

float shutterMask(float u,float v,float radius,float soft){
    if(ShutterEnabled<0.5)return 1.0;
    vec2 p=vec2(u,v)/max(radius,0.0001);
    float cr=cos(-FrameRotation);
    float sr=sin(-FrameRotation);
    vec2 q=vec2(p.x*cr-p.y*sr,p.x*sr+p.y*cr);
    float m=1.0;
    m*=bladeMask(q,vec2(0.0,1.0),BladeA.x,BladeB.x,soft);
    m*=bladeMask(q,vec2(1.0,0.0),BladeA.y,BladeB.y,soft);
    m*=bladeMask(q,vec2(0.0,-1.0),BladeA.z,BladeB.z,soft);
    m*=bladeMask(q,vec2(-1.0,0.0),BladeA.w,BladeB.w,soft);
    return m;
}

in vec4 VertexColor;
out vec4 fragColor;

vec3 reconstructViewPos(vec2 uv,float depth){
    vec4 clip=vec4(uv*2.0-1.0,depth*2.0-1.0,1.0);
    vec4 view=InvProjMat*clip;
    return view.xyz/max(view.w,0.000001);
}

vec4 sampleGoboBlur(sampler2D samp, vec2 uv, float blur){
    if(blur<=0.00001)return texture(samp,uv);

    float b=blur;
    float s=b*0.70710678;

    vec4 c=texture(samp,uv)*0.20;

    c+=texture(samp,uv+vec2(b,0.0))*0.10;
    c+=texture(samp,uv-vec2(b,0.0))*0.10;
    c+=texture(samp,uv+vec2(0.0,b))*0.10;
    c+=texture(samp,uv-vec2(0.0,b))*0.10;

    c+=texture(samp,uv+vec2(s,s))*0.075;
    c+=texture(samp,uv+vec2(-s,s))*0.075;
    c+=texture(samp,uv+vec2(s,-s))*0.075;
    c+=texture(samp,uv+vec2(-s,-s))*0.075;

    c+=texture(samp,uv+vec2(b*1.5,0.0))*0.025;
    c+=texture(samp,uv-vec2(b*1.5,0.0))*0.025;
    c+=texture(samp,uv+vec2(0.0,b*1.5))*0.025;
    c+=texture(samp,uv-vec2(0.0,b*1.5))*0.025;

    return c;
}

void main(){
    vec2 screenUV=gl_FragCoord.xy/ScreenSize;
    float depth=texture(Sampler1,screenUV).r;

    if(depth>=0.99999)discard;

    vec3 surfacePos=reconstructViewPos(screenUV,depth);
    vec3 toSurface=surfacePos-LightPos;
    vec3 dir=normalize(LightDir);

    float zDist=dot(toSurface,dir);
    float maxDist=min(MaxLen,MaxGoboDist);

    if(zDist<=0.0||zDist>maxDist)discard;

    if(OcclusionEnabled>0.5){
        vec3 surfaceRay=toSurface;
        float surfaceDistance=length(surfaceRay);

        if(surfaceDistance>0.0001){
            vec3 surfaceRayDir=surfaceRay/surfaceDistance;
            float denominator=dot(surfaceRayDir,OcclusionNormal);

            if(abs(denominator)>0.00001){
                float hitT=dot(OcclusionPos-LightPos,OcclusionNormal)/denominator;
                // Tolerance proportionnelle a la distance : la profondeur reconstruite depuis le
                // depth buffer n'est precise qu'a quelques cm a 10 blocs, une marge fixe de 1 mm
                // faisait clignoter des pixels de la surface eclairee elle-meme (z-fighting).
                float occlusionBias=max(0.08,surfaceDistance*0.03);
                if(hitT>0.001&&hitT<surfaceDistance-occlusionBias)discard;
            }
        }
    }

    vec3 uAxis=normalize(AxisU);
    vec3 vAxis=normalize(AxisV);

    float u=dot(toSurface,uAxis);
    float v=dot(toSurface,vAxis);

    float angle=radians(GoboRotation);
    float ca=cos(angle);
    float sa=sin(angle);

    float radius=max(BaseRadius+zDist*max(TanHalfAngle,0.0001),0.0001);
    float slotDistance=radius*2.0;

    // Desplazamiento de rueda a nivel "puerta focal" (Fija en la montura) antes de rotar
    float localU_A=u+WheelTransition*slotDistance;
    float localV_A=v;
    float ruA=localU_A*ca-localV_A*sa;
    float rvA=localU_A*sa+localV_A*ca;
    vec2 goboUVA=vec2(ruA/radius*0.5+0.5,1.0-(rvA/radius*0.5+0.5));

    float localU_B=u-(1.0-WheelTransition)*slotDistance;
    float localV_B=v;
    float ruB=localU_B*ca-localV_B*sa;
    float rvB=localU_B*sa+localV_B*ca;
    vec2 goboUVB=vec2(ruB/radius*0.5+0.5,1.0-(rvB/radius*0.5+0.5));

    // Máscara cónica fija física
    float radial=length(vec2(u,v))/radius;
    if(radial>=1.0)discard;

    float focusNorm=clamp(Focus,0.0,1.0);
    float blurAmount=focusNorm*focusNorm*0.045;

    // Sampling Gobo A
    float goboAlphaA=0.0;
    vec3 goboColorA=vec3(0.0);
    if(goboUVA.x>=0.0&&goboUVA.x<=1.0&&goboUVA.y>=0.0&&goboUVA.y<=1.0){
        vec4 sampleA=sampleGoboBlur(Sampler0,goboUVA,blurAmount);
        goboColorA=sampleA.rgb;
        goboAlphaA=sampleA.a*dot(goboColorA,vec3(0.299,0.587,0.114));
    }

    // Sampling Gobo B
    float goboAlphaB=0.0;
    vec3 goboColorB=vec3(0.0);
    if(goboUVB.x>=0.0&&goboUVB.x<=1.0&&goboUVB.y>=0.0&&goboUVB.y<=1.0){
        vec4 sampleB=sampleGoboBlur(Sampler2,goboUVB,blurAmount);
        goboColorB=sampleB.rgb;
        goboAlphaB=sampleB.a*dot(goboColorB,vec3(0.299,0.587,0.114));
    }

    float goboAlpha=goboAlphaA+goboAlphaB;

    // Couteaux : le bord est net au point (focus 0) et s'adoucit avec le defocus, comme un vrai profile.
    float shutterSoft=0.012+focusNorm*0.06;
    goboAlpha*=shutterMask(u,v,radius,shutterSoft);
    goboAlpha*=animationMask(u,v,radius);
    // Ombre portee sur la surface : blocs et entites entre le point et la source.
    vec3 wposS=LightPosW+zDist*LightDirW+u*AxisUW+v*AxisVW;
    goboAlpha*=shadowFactor(wposS,LightPosW,VoxelCell*1.05);
    if(goboAlpha<=0.001)discard;

    // Mezcla de interpolación física
    vec3 goboColor=(goboColorA*goboAlphaA+goboColorB*goboAlphaB)/goboAlpha;

    float edgeStart=mix(0.88,0.72,focusNorm);
    float edgeFade=1.0-smoothstep(edgeStart,1.0,radial);

    float finalFactor=goboAlpha*edgeFade*Intensity*GOBO_BRIGHTNESS*VertexColor.a;

    if(finalFactor<=0.001)discard;

    vec3 finalColor=LightColor*goboColor*finalFactor;

    fragColor=vec4(finalColor,finalFactor);
}