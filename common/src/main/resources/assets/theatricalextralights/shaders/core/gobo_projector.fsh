#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
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
uniform float Intensity;
uniform float Focus;
uniform vec3 LightColor;
uniform vec2 ScreenSize;
uniform float OcclusionEnabled;

const float GOBO_BRIGHTNESS=0.55;

in vec4 VertexColor;
out vec4 fragColor;

vec3 reconstructViewPos(vec2 uv,float depth){
    vec4 clip=vec4(uv*2.0-1.0,depth*2.0-1.0,1.0);
    vec4 view=InvProjMat*clip;
    return view.xyz/max(view.w,0.000001);
}

vec4 sampleGoboBlur(vec2 uv,float blur){
    if(blur<=0.00001)return texture(Sampler0,uv);

    float b=blur;
    float s=b*0.70710678;

    vec4 c=texture(Sampler0,uv)*0.20;

    c+=texture(Sampler0,uv+vec2(b,0.0))*0.10;
    c+=texture(Sampler0,uv-vec2(b,0.0))*0.10;
    c+=texture(Sampler0,uv+vec2(0.0,b))*0.10;
    c+=texture(Sampler0,uv-vec2(0.0,b))*0.10;

    c+=texture(Sampler0,uv+vec2(s,s))*0.075;
    c+=texture(Sampler0,uv+vec2(-s,s))*0.075;
    c+=texture(Sampler0,uv+vec2(s,-s))*0.075;
    c+=texture(Sampler0,uv+vec2(-s,-s))*0.075;

    c+=texture(Sampler0,uv+vec2(b*1.5,0.0))*0.025;
    c+=texture(Sampler0,uv-vec2(b*1.5,0.0))*0.025;
    c+=texture(Sampler0,uv+vec2(0.0,b*1.5))*0.025;
    c+=texture(Sampler0,uv-vec2(0.0,b*1.5))*0.025;

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

    float rotatedU=u*ca-v*sa;
    float rotatedV=u*sa+v*ca;

    float radius=max(BaseRadius+zDist*max(TanHalfAngle,0.0001),0.0001);

    vec2 goboUV=vec2(
        rotatedU/radius*0.5+0.5,
        1.0-(rotatedV/radius*0.5+0.5)
    );

    if(goboUV.x<0.0||goboUV.x>1.0||goboUV.y<0.0||goboUV.y>1.0)discard;

    float radial=length(vec2(u,v))/radius;

    if(radial>=1.0)discard;

    float focusNorm=clamp(Focus,0.0,1.0);
    float blurAmount=focusNorm*focusNorm*0.045;

    vec4 goboSample=sampleGoboBlur(goboUV,blurAmount);
    vec3 goboColor=goboSample.rgb;

    float goboLuma=dot(goboColor,vec3(0.299,0.587,0.114));
    float goboAlpha=goboSample.a*goboLuma;

    if(goboAlpha<=0.001)discard;

    // Sharp spotlight edge at Focus 0.
    // Focus only blurs the gobo texture, not the spot geometry.
    float edgeStart=mix(0.88,0.72,focusNorm);
    float edgeFade=1.0-smoothstep(edgeStart,1.0,radial);

    float finalFactor=goboAlpha*edgeFade*Intensity*GOBO_BRIGHTNESS*VertexColor.a;

    if(finalFactor<=0.001)discard;

    vec3 finalColor=LightColor*goboColor*finalFactor;

    fragColor=vec4(finalColor,finalFactor);
}