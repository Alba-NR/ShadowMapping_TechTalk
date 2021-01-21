#version 330 core

struct Material {
    sampler2D diffuse_tex1;    // diffuse map (for diffuse colour)
    sampler2D specular_tex1;   // specular map (for specular reflection)
    vec3 diffuseColour;        // diffuse colour
    vec3 specularColour;       // specular colour
    float K_a;          // ambient reflection coefficient
    float K_diff;       // diff reflection coeff
    float K_spec;       // spec reflection coeff
    float shininess;    // shininness coeff (for specular reflection)
};

struct DirLight { // directional light in scene (1 atm)
    vec3 colour;        // light colour
    vec3 direction;     // light direction
    float strength;     // light strength/intensity
};

struct PointLight { // point light
    vec3 position;      // light pos in wc
    vec3 colour;        // light colour
    float strength;     // light strength/intensity

    float constant;     // constants for impl attenuation
    float linear;
    float quadratic;
};


struct SpotLight { // flash light (spotlight)
    vec3 position;      // light pos in wc
    vec3 colour;        // light colour
    float strength;     // light strength/intensity
    vec3 direction;     // light direction
    float cutoffCosine; // cosine of spotlight cutoff angle
    float outerCutoffCosine; // cosine of outer spotlight cutoff angle (for softer borders)

    float constant;     // constants for impl attenuation
    float linear;
    float quadratic;
};

in VS_OUT {
    vec2 TexCoords;   // texture UV coord
    vec3 wc_normal;  // fragment normal in world coord
    vec3 wc_fragPos; // fragment position in world coord
} fs_in;

out vec4 FragColor;

uniform vec3 I_a;
uniform DirLight dirLight;
uniform Material material;
uniform bool materialUsesTextures;
uniform vec3 wc_cameraPos;


// function prototypes
vec3 CalcDirLight(DirLight light, vec3 N, vec3 V, vec3 diffColour, vec3 specColour);
vec3 toneMapAndDisplayEncode(vec3 linearRGB);

void main()
{
    vec3 I_result;

    // calc vectors
    vec3 N = normalize(fs_in.wc_normal);
    vec3 V = normalize(wc_cameraPos - fs_in.wc_fragPos);

    // get diffuse & specular colours...
    vec3 diffColour, specColour, diffComponent, specComponent;
    if(materialUsesTextures){
        // ...from textures (the maps...)
        diffColour = vec3(texture(material.diffuse_tex1, fs_in.TexCoords));
        specColour = vec3(texture(material.specular_tex1, fs_in.TexCoords));
    } else {
        diffColour = material.diffuseColour;
        specColour = material.specularColour;
    }
    diffComponent = diffColour * material.K_diff;
    specComponent = specColour * material.K_spec;

    // Directional lighting
    I_result = CalcDirLight(dirLight, N, V, diffComponent, specComponent);

    // ambient light
    I_result += I_a * diffColour * material.K_a;

    // perform basic tonemapping (adjust brightness) and display encoding (apply gamma correction)
    FragColor = vec4(toneMapAndDisplayEncode(I_result), 1.0);
}

// performs tone mapping (-- here: brightness adjustment) and display encoding (-- here: gamma correction) combined
vec3 toneMapAndDisplayEncode(vec3 linearRGB)
{
    float L_white = 0.7; // scene-referrered luminance of white (controls brightness of img)
    float inverseGamma = 1.0/2.2;   // gamma value is 2.2

    return pow(linearRGB / L_white, vec3(inverseGamma));
}

vec3 CalcDirLight(DirLight light, vec3 N, vec3 V, vec3 diffComponent, vec3 specComponent)
{
    // calc vectors
    vec3 L = normalize(-light.direction);
    vec3 H = normalize(N + V); // halfway vector (half way btwn V & N)

    // diffuse & specular shading
    vec3 I_diffuse = light.colour * diffComponent * max(dot(N, L), 0.0);
    vec3 I_specular = light.colour * specComponent * pow(max(dot(N, H), 0.0), material.shininess);

    return (I_diffuse + I_specular) * light.strength;
}