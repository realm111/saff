package pl.tpahere.auto;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TpaHereScreen extends Screen {
    private static final int W = 240;
    private static final int H = 176;

    private ButtonWidget toggleButton;

    public TpaHereScreen() {
        super(Text.translatable("tpahere.title"));
    }

    private Text toggleText() {
        return Text.translatable(Config.enabled ? "tpahere.on" : "tpahere.off")
                .formatted(Config.enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = (this.height - H) / 2;

        toggleButton = addDrawableChild(ButtonWidget.builder(toggleText(), b -> {
            Config.enabled = !Config.enabled;
            Config.save();
            b.setMessage(toggleText());
        }).dimensions(cx - 100, top + 56, 200, 28).build());

        // Odstep miedzy prosbami
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            Config.delaySeconds = Math.max(1, Config.delaySeconds - 1);
            Config.save();
        }).dimensions(cx - 100, top + 100, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            Config.delaySeconds = Math.min(60, Config.delaySeconds + 1);
            Config.save();
        }).dimensions(cx + 80, top + 100, 20, 20).build());

        // Cooldown na tego samego gracza
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            Config.cooldownMinutes = Math.max(1, Config.cooldownMinutes - 1);
            Config.save();
        }).dimensions(cx - 100, top + 124, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            Config.cooldownMinutes = Math.min(120, Config.cooldownMinutes + 1);
            Config.save();
        }).dimensions(cx + 80, top + 124, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("tpahere.close"), b -> close())
                .dimensions(cx - 50, top + H - 26, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int left = cx - W / 2;
        int top = (this.height - H) / 2;
        int accent = Config.enabled ? 0xFF1E8A44 : 0xFF8A1E1E;

        // ramka + panel + naglowek
        context.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF4A4A52);
        context.fill(left, top, left + W, top + H, 0xF01B1B20);
        context.fill(left, top, left + W, top + 24, accent);
        context.fill(left, top + 24, left + W, top + 25, 0xFF000000);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, top + 8, 0xFFFFFFFF);

        Text status = Text.translatable(Config.enabled ? "tpahere.status_on" : "tpahere.status_off");
        context.drawCenteredTextWithShadow(this.textRenderer, status, cx, top + 38, 0xFFBBBBBB);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("tpahere.delay", Config.delaySeconds), cx, top + 106, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("tpahere.cooldown", Config.cooldownMinutes), cx, top + 130, 0xFFFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
