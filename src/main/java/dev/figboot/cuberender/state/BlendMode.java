package dev.figboot.cuberender.state;

import dev.figboot.cuberender.math.Vector4f;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum BlendMode {
    DISABLE((inOutColor, prev) -> inOutColor.w = 1),
    BINARY((inOutColor, prev) -> {
        if (inOutColor.w < 0.5) {
            inOutColor.copyFrom(prev);
        } else {
            inOutColor.w = 1;
        }
    }),
    BLEND_OVER((inOutColor, prev) -> {
        float pAlphaFactor = prev.w * (1 - inOutColor.w);
        float aOut = inOutColor.w + pAlphaFactor;

        inOutColor.x = (inOutColor.x * inOutColor.w + prev.x * pAlphaFactor) / aOut;
        inOutColor.y = (inOutColor.y * inOutColor.w + prev.y * pAlphaFactor) / aOut;
        inOutColor.z = (inOutColor.z * inOutColor.w + prev.z * pAlphaFactor) / aOut;
        inOutColor.w = aOut;
    });

    private final BlendFunction function;

    public interface BlendFunction {
        void blend(Vector4f inOutColor, Vector4f prev);
    }
}
