package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.fs.starfarer.api.ui.PositionAPI;

import static wfg.native_ui.util.UIConstants.screenH;
import static wfg.native_ui.util.UIConstants.screenW;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BloomEffect {
    private FBObject sceneFBO;
    private FBObject brightPassFBO;
    private FBObject blurFBO1;
    private FBObject blurFBO2;
    
    private int brightPassShader;
    private int blurShader;
    private int combineShader;
    
    private boolean initialized = false;
    private boolean inCapture = false;

    private int prevFboX = 0;
    private int prevFboY = 0;
    private int prevFboW = 0;
    private int prevFboH = 0;
    private int prevFBO = 0;

    private int panelX = 0;
    private int panelY = 0;
    private int panelW = 0;
    private int panelH = 0;

    private class FBObject {
        final int fbo;
        final int texture;
        final int width;
        final int height;
        
        FBObject(int width, int height, boolean linearFilter) {
            this.width = width;
            this.height = height;
            
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, 
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null
            );
            
            final int filter = linearFilter ? GL11.GL_LINEAR : GL11.GL_NEAREST;
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            
            fbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, 
                GL11.GL_TEXTURE_2D, texture, 0
            );
            
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("FBO creation failed: " + status);
            }
            
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
        
        void cleanup() {
            GL30.glDeleteFramebuffers(fbo);
            GL11.glDeleteTextures(texture);
        }
    }
    
    public final boolean isInitialized() {
        return initialized;
    }

    public final void init(PositionAPI pos) {
        init((int) pos.getX(), (int) pos.getY(), (int) pos.getWidth(), (int) pos.getHeight());
    }

    public final void init(int x, int y, int w, int h) {
        panelX = x;
        panelY = y;
        panelW = w;
        panelH = h;

        sceneFBO = new FBObject(w, h, false);
        brightPassFBO = new FBObject(w / 2, h / 2, true);
        blurFBO1 = new FBObject(w / 2, h / 2, true);
        blurFBO2 = new FBObject(w / 2, h / 2, true);
        
        initShaders();
        initialized = true;
    }
    
    private void initShaders() {
        // Bright pass shader - extracts bright pixels
        final String brightPassVertex = 
            "#version 120\n" +
            "void main() {\n" +
            "    gl_Position = ftransform();\n" +
            "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "}";
        
        final String brightPassFragment = 
            "#version 120\n" +
            "uniform sampler2D sceneTex;\n" +
            "uniform float threshold = 0.8;\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(sceneTex, gl_TexCoord[0].xy);\n" +
            "    float brightness = (color.r * 0.2126 + color.g * 0.7152 + color.b * 0.0722);\n" +
            "    if (brightness > threshold) {\n" +
            "        gl_FragColor = color;\n" +
            "    } else {\n" +
            "        gl_FragColor = vec4(0.0);\n" +
            "    }\n" +
            "}";
        
        // Gaussian blur shader
        final String blurVertex = 
            "#version 120\n" +
            "void main() {\n" +
            "    gl_Position = ftransform();\n" +
            "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "}";
        
        final String blurFragment = 
            "#version 120\n" +
            "uniform sampler2D tex;\n" +
            "uniform vec2 pixelSize;\n" +
            "uniform int horizontal;\n" +
            "void main() {\n" +
            "    vec2 uv = gl_TexCoord[0].xy;\n" +
            "    vec4 color = vec4(0.0);\n" +
            "    \n" +
            "    // Gaussian weights (7-tap)\n" +
            "    float weights[7] = float[7](0.00598, 0.060626, 0.241843, 0.383103, 0.241843, 0.060626, 0.00598);\n" +
            "    \n" +
            "    if (horizontal == 1) {\n" +
            "        for (int i = -3; i <= 3; i++) {\n" +
            "            vec2 offset = vec2(pixelSize.x * float(i), 0.0);\n" +
            "            color += texture2D(tex, uv + offset) * weights[i+3];\n" +
            "        }\n" +
            "    } else {\n" +
            "        for (int i = -3; i <= 3; i++) {\n" +
            "            vec2 offset = vec2(0.0, pixelSize.y * float(i));\n" +
            "            color += texture2D(tex, uv + offset) * weights[i+3];\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    gl_FragColor = color;\n" +
            "}";
        
        // Combine shader - blends bloom with original
        final String combineVertex = 
            "#version 120\n" +
            "void main() {\n" +
            "    gl_Position = ftransform();\n" +
            "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "}";
        
        final String combineFragment = 
            "#version 120\n" +
            "uniform sampler2D sceneTex;\n" +
            "uniform sampler2D bloomTex;\n" +
            "uniform float intensity = 1.0;\n" +
            "void main() {\n" +
            "    vec4 sceneColor = texture2D(sceneTex, gl_TexCoord[0].xy);\n" +
            "    vec4 bloomColor = texture2D(bloomTex, gl_TexCoord[0].xy);\n" +
            "    gl_FragColor = sceneColor + bloomColor * intensity;\n" +
            "}";
        
        brightPassShader = createShaderProgram(brightPassVertex, brightPassFragment);
        blurShader = createShaderProgram(blurVertex, blurFragment);
        combineShader = createShaderProgram(combineVertex, combineFragment);
    }
    
    private int createShaderProgram(String vertexSource, String fragmentSource) {
        final int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSource);
        GL20.glCompileShader(vertexShader);
        
        final IntBuffer result = BufferUtils.createIntBuffer(1);
        GL20.glGetShader(vertexShader, GL20.GL_COMPILE_STATUS, result);
        if (result.get(0) == GL11.GL_FALSE) {
            final String log = GL20.glGetShaderInfoLog(vertexShader, GL20.GL_INFO_LOG_LENGTH);
            throw new RuntimeException("Vertex shader compilation failed: " + log);
        }
        
        final int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSource);
        GL20.glCompileShader(fragmentShader);
        
        GL20.glGetShader(fragmentShader, GL20.GL_COMPILE_STATUS, result);
        if (result.get(0) == GL11.GL_FALSE) {
            final String log = GL20.glGetShaderInfoLog(fragmentShader, GL20.GL_INFO_LOG_LENGTH);
            throw new RuntimeException("Fragment shader compilation failed: " + log);
        }
        
        final int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        
        GL20.glGetProgram(program, GL20.GL_LINK_STATUS, result);
        if (result.get(0) == GL11.GL_FALSE) {
            final String log = GL20.glGetProgramInfoLog(program, GL20.GL_INFO_LOG_LENGTH);
            throw new RuntimeException("Program linking failed: " + log);
        }
        
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        
        return program;
    }
    
    public void beginSceneCapture() {
        if (!initialized) throw new IllegalStateException("BloomEffect not initialized");

        // Store previous FBO and viewport
        prevFBO = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        IntBuffer vp = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_VIEWPORT, vp);
        prevFboX = vp.get(0);
        prevFboY = vp.get(1);
        prevFboW = vp.get(2);
        prevFboH = vp.get(3);

        // Push both matrices AND attributes
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        // Bind our FBO and set viewport
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, sceneFBO.fbo);
        GL11.glViewport(0, 0, sceneFBO.width, sceneFBO.height);

        GL11.glColorMask(true, true, true, true);
        // GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, panelW, 0, panelH, -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        inCapture = true;
    }

    public void endSceneCapture() {
        if (!initialized || !inCapture) return;

        // Restore FBO and viewport
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFBO);
        GL11.glViewport(prevFboX, prevFboY, prevFboW, prevFboH);

        // Restore matrices (pop in reverse order)
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        // Restore attributes
        GL11.glPopAttrib();

        GL11.glMatrixMode(GL11.GL_MODELVIEW); // Reset to modelview as default
        inCapture = false;
    }

    public void render() {
        render(screenW, screenH, panelX, panelY);
    }

    public void render(int targetWidth, int targetHeight, int panelX, int panelY) {
        if (!initialized) throw new IllegalStateException("BloomEffect not initialized");

        // Save current state
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, targetWidth, 0, targetHeight, -1, 1); // Screen coordinates
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 1. Bright pass extraction (render full scene texture to half-size FBO)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, brightPassFBO.fbo);
        GL11.glViewport(0, 0, brightPassFBO.width, brightPassFBO.height);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(brightPassShader);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFBO.texture);
        renderQuad(0, 0, brightPassFBO.width, brightPassFBO.height);

        // 2. Horizontal blur
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, blurFBO1.fbo);
        GL11.glViewport(0, 0, blurFBO1.width, blurFBO1.height);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(blurShader);
        GL20.glUniform1i(GL20.glGetUniformLocation(blurShader, "horizontal"), 1);
        GL20.glUniform2f(GL20.glGetUniformLocation(blurShader, "pixelSize"),
                1.0f / blurFBO1.width, 1.0f / blurFBO1.height);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, brightPassFBO.texture);
        renderQuad(0, 0, blurFBO1.width, blurFBO1.height);

        // 3. Vertical blur
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, blurFBO2.fbo);
        GL11.glViewport(0, 0, blurFBO2.width, blurFBO2.height);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUniform1i(GL20.glGetUniformLocation(blurShader, "horizontal"), 0);
        GL20.glUniform2f(GL20.glGetUniformLocation(blurShader, "pixelSize"),
                1.0f / blurFBO2.width, 1.0f / blurFBO2.height);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurFBO1.texture);
        renderQuad(0, 0, blurFBO2.width, blurFBO2.height);

        // 4. Combine original and bloom at panel position on screen
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, targetWidth, targetHeight);

        GL20.glUseProgram(combineShader);
        GL20.glUniform1i(GL20.glGetUniformLocation(combineShader, "sceneTex"), 0);
        GL20.glUniform1i(GL20.glGetUniformLocation(combineShader, "bloomTex"), 1);
        GL20.glUniform1f(GL20.glGetUniformLocation(combineShader, "intensity"), 1.2f);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFBO.texture);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurFBO2.texture);

        // Draw the combined quad at panel position (using screen coordinates)
        renderQuad(panelX, panelY, panelW, panelH);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUseProgram(0);

        // Restore state
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void renderQuad(int x, int y, int width, int height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    public void cleanup() {
        if (sceneFBO != null) sceneFBO.cleanup();
        if (brightPassFBO != null) brightPassFBO.cleanup();
        if (blurFBO1 != null) blurFBO1.cleanup();
        if (blurFBO2 != null) blurFBO2.cleanup();
        
        GL20.glDeleteProgram(brightPassShader);
        GL20.glDeleteProgram(blurShader);
        GL20.glDeleteProgram(combineShader);
    }
}