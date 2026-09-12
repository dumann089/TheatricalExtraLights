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
                if(hitT>0.001&&hitT<surfaceDistance-0.001)discard;
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