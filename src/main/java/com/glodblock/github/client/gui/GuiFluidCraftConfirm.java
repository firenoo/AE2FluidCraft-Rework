package com.glodblock.github.client.gui;

import java.text.NumberFormat;
import java.util.*;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import appeng.api.AEApi;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.core.AELog;
import appeng.core.localization.GuiText;
import appeng.integration.modules.NEI;
import appeng.util.Platform;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.client.gui.container.ContainerFluidCraftConfirm;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.common.parts.PartFluidPatternTerminal;
import com.glodblock.github.common.parts.PartFluidPatternTerminalEx;
import com.glodblock.github.coremod.hooker.CoreModHooks;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.inventory.item.IWirelessTerminal;
import com.glodblock.github.inventory.item.WirelessPatternTerminalInventory;
import com.glodblock.github.network.CPacketFluidPatternTermBtns;
import com.glodblock.github.network.CPacketSwitchGuis;
import com.glodblock.github.util.ModAndClassUtil;
import com.google.common.base.Joiner;

public class GuiFluidCraftConfirm extends AEBaseGui {

    private final ContainerFluidCraftConfirm ccc;
    private final int rows = 5;

    private final IItemList<IAEItemStack> storage = AEApi.instance().storage().createItemList();
    private final IItemList<IAEItemStack> pending = AEApi.instance().storage().createItemList();
    private final IItemList<IAEItemStack> missing = AEApi.instance().storage().createItemList();

    private final List<IAEItemStack> visual = new ArrayList<IAEItemStack>();

    private GuiType OriginalGui;
    private GuiButton cancel;
    private GuiButton start;
    private GuiButton selectCPU;
    private int tooltip = -1;
    private ItemStack hoveredStack;

    public GuiFluidCraftConfirm(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerFluidCraftConfirm(inventoryPlayer, te));
        this.xSize = 238;
        this.ySize = 206;

        final GuiScrollbar scrollbar = new GuiScrollbar();
        this.setScrollBar(scrollbar);

        this.ccc = (ContainerFluidCraftConfirm) this.inventorySlots;

        if (te instanceof PartFluidPatternTerminal) {
            this.OriginalGui = GuiType.FLUID_PATTERN_TERMINAL;
        } else if (te instanceof PartFluidPatternTerminalEx) {
            this.OriginalGui = GuiType.FLUID_PATTERN_TERMINAL_EX;
        } else if (te instanceof IWirelessTerminal && ((IWirelessTerminal) te).isUniversal(te)) {
            this.OriginalGui = ItemWirelessUltraTerminal.readMode(((IWirelessTerminal) te).getItemStack());
        } else if (te instanceof WirelessPatternTerminalInventory) {
            this.OriginalGui = GuiType.FLUID_TERMINAL;
        }
    }

    boolean isAutoStart() {
        return ((ContainerFluidCraftConfirm) this.inventorySlots).isAutoStart();
    }

    @Override
    public void initGui() {
        super.initGui();

        this.start = new GuiButton(
                0,
                this.guiLeft + 162,
                this.guiTop + this.ySize - 25,
                50,
                20,
                GuiText.Start.getLocal());
        this.start.enabled = false;
        this.buttonList.add(this.start);

        this.selectCPU = new GuiButton(
                0,
                this.guiLeft + (219 - 180) / 2,
                this.guiTop + this.ySize - 68,
                180,
                20,
                GuiText.CraftingCPU.getLocal() + ": " + GuiText.Automatic);
        this.selectCPU.enabled = false;
        this.buttonList.add(this.selectCPU);

        if (this.OriginalGui != null) {
            this.cancel = new GuiButton(
                    0,
                    this.guiLeft + 6,
                    this.guiTop + this.ySize - 25,
                    50,
                    20,
                    GuiText.Cancel.getLocal());
        }

        this.buttonList.add(this.cancel);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float btn) {
        this.updateCPUButtonText();

        this.start.enabled = !(this.ccc.hasNoCPU() || this.isSimulation());
        this.selectCPU.enabled = !this.isSimulation();

        final int gx = (this.width - this.xSize) / 2;
        final int gy = (this.height - this.ySize) / 2;

        this.tooltip = -1;

        final int offY = 23;
        int y = 0;
        int x = 0;
        for (int z = 0; z <= 4 * 5; z++) {
            final int minX = gx + 9 + x * 67;
            final int minY = gy + 22 + y * offY;

            if (minX < mouseX && minX + 67 > mouseX) {
                if (minY < mouseY && minY + offY - 2 > mouseY) {
                    this.tooltip = z;
                    break;
                }
            }

            x++;

            if (x > 2) {
                y++;
                x = 0;
            }
        }

        super.drawScreen(mouseX, mouseY, btn);
    }

    private void updateCPUButtonText() {
        String btnTextText = GuiText.CraftingCPU.getLocal() + ": " + GuiText.Automatic.getLocal();
        if (this.ccc.getSelectedCpu() >= 0) // && status.selectedCpu < status.cpus.size() )
        {
            if (this.ccc.getName().length() > 0) {
                final String name = this.ccc.getName().substring(0, Math.min(20, this.ccc.getName().length()));
                btnTextText = GuiText.CraftingCPU.getLocal() + ": " + name;
            } else {
                btnTextText = GuiText.CraftingCPU.getLocal() + ": #" + this.ccc.getSelectedCpu();
            }
        }

        if (this.ccc.hasNoCPU()) {
            btnTextText = GuiText.NoCraftingCPUs.getLocal();
        }

        this.selectCPU.displayString = btnTextText;
    }

    private boolean isSimulation() {
        return ((ContainerFluidCraftConfirm) this.inventorySlots).isSimulation();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        final long BytesUsed = this.ccc.getUsedBytes();
        final String byteUsed = NumberFormat.getInstance().format(BytesUsed);
        final String Add = BytesUsed > 0 ? (byteUsed + ' ' + GuiText.BytesUsed.getLocal())
                : GuiText.CalculatingWait.getLocal();
        this.fontRendererObj.drawString(GuiText.CraftingPlan.getLocal() + " - " + Add, 8, 7, 4210752);

        String dsp = null;

        if (this.isSimulation()) {
            dsp = GuiText.Simulation.getLocal();
        } else {
            dsp = this.ccc.getCpuAvailableBytes() > 0
                    ? (GuiText.Bytes.getLocal() + ": "
                            + this.ccc.getCpuAvailableBytes()
                            + " : "
                            + GuiText.CoProcessors.getLocal()
                            + ": "
                            + this.ccc.getCpuCoProcessors())
                    : GuiText.Bytes.getLocal() + ": N/A : " + GuiText.CoProcessors.getLocal() + ": N/A";
        }

        final int offset = (219 - this.fontRendererObj.getStringWidth(dsp)) / 2;
        this.fontRendererObj.drawString(dsp, offset, 165, 4210752);

        final int sectionLength = 67;

        int x = 0;
        int y = 0;
        final int xo = 9;
        final int yo = 22;
        final int viewStart = this.getScrollBar().getCurrentScroll() * 3;
        final int viewEnd = viewStart + 3 * this.rows;

        String dspToolTip = "";
        final List<String> lineList = new LinkedList<String>();
        int toolPosX = 0;
        int toolPosY = 0;
        hoveredStack = null;
        final int offY = 23;

        for (int z = viewStart; z < Math.min(viewEnd, this.visual.size()); z++) {
            final IAEItemStack refStack = this.visual.get(z); // repo.getReferenceItem( z );
            if (refStack != null) {
                GL11.glPushMatrix();
                GL11.glScaled(0.5, 0.5, 0.5);

                final IAEItemStack stored = this.storage.findPrecise(refStack);
                final IAEItemStack pendingStack = this.pending.findPrecise(refStack);
                final IAEItemStack missingStack = this.missing.findPrecise(refStack);

                int lines = 0;

                if (stored != null && stored.getStackSize() > 0) {
                    lines++;
                }
                if (pendingStack != null && pendingStack.getStackSize() > 0) {
                    lines++;
                }
                if (pendingStack != null && pendingStack.getStackSize() > 0) {
                    lines++;
                }

                final int negY = ((lines - 1) * 5) / 2;
                int downY = 0;

                if (stored != null && stored.getStackSize() > 0) {
                    String str = Long.toString(stored.getStackSize());
                    if (stored.getStackSize() >= 10000) {
                        str = Long.toString(stored.getStackSize() / 1000) + 'k';
                    }
                    if (stored.getStackSize() >= 10000000) {
                        str = Long.toString(stored.getStackSize() / 1000000) + 'm';
                    }

                    str = GuiText.FromStorage.getLocal() + ": " + str;
                    final int w = 4 + this.fontRendererObj.getStringWidth(str);
                    this.fontRendererObj.drawString(
                            str,
                            (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - (w * 0.5)) * 2),
                            (y * offY + yo + 6 - negY + downY) * 2,
                            4210752);

                    if (this.tooltip == z - viewStart) {
                        lineList.add(GuiText.FromStorage.getLocal() + ": " + stored.getStackSize());
                    }

                    downY += 5;
                }

                boolean red = false;
                if (missingStack != null && missingStack.getStackSize() > 0) {
                    String str = Long.toString(missingStack.getStackSize());
                    if (missingStack.getStackSize() >= 10000) {
                        str = Long.toString(missingStack.getStackSize() / 1000) + 'k';
                    }
                    if (missingStack.getStackSize() >= 10000000) {
                        str = Long.toString(missingStack.getStackSize() / 1000000) + 'm';
                    }

                    str = GuiText.Missing.getLocal() + ": " + str;
                    final int w = 4 + this.fontRendererObj.getStringWidth(str);
                    this.fontRendererObj.drawString(
                            str,
                            (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - (w * 0.5)) * 2),
                            (y * offY + yo + 6 - negY + downY) * 2,
                            4210752);

                    if (this.tooltip == z - viewStart) {
                        lineList.add(GuiText.Missing.getLocal() + ": " + missingStack.getStackSize());
                    }

                    red = true;
                    downY += 5;
                }

                if (pendingStack != null && pendingStack.getStackSize() > 0) {
                    String str = Long.toString(pendingStack.getStackSize());
                    if (pendingStack.getStackSize() >= 10000) {
                        str = Long.toString(pendingStack.getStackSize() / 1000) + 'k';
                    }
                    if (pendingStack.getStackSize() >= 10000000) {
                        str = Long.toString(pendingStack.getStackSize() / 1000000) + 'm';
                    }

                    str = GuiText.ToCraft.getLocal() + ": " + str;
                    final int w = 4 + this.fontRendererObj.getStringWidth(str);
                    this.fontRendererObj.drawString(
                            str,
                            (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - (w * 0.5)) * 2),
                            (y * offY + yo + 6 - negY + downY) * 2,
                            4210752);

                    if (this.tooltip == z - viewStart) {
                        lineList.add(GuiText.ToCraft.getLocal() + ": " + Long.toString(pendingStack.getStackSize()));
                    }
                }

                GL11.glPopMatrix();
                final int posX = x * (1 + sectionLength) + xo + sectionLength - 19;
                final int posY = y * offY + yo;

                final ItemStack is = CoreModHooks.displayFluid(refStack.copy());

                if (this.tooltip == z - viewStart) {
                    dspToolTip = Platform.getItemDisplayName(is);
                    if (lineList.size() > 0) {
                        addItemTooltip(is, lineList);
                        dspToolTip = dspToolTip + '\n' + Joiner.on("\n").join(lineList);
                    }

                    toolPosX = x * (1 + sectionLength) + xo + sectionLength - 8;
                    toolPosY = y * offY + yo;
                    hoveredStack = is;
                }

                this.drawItem(posX, posY, is);

                if (red) {
                    final int startX = x * (1 + sectionLength) + xo;
                    final int startY = posY - 4;
                    drawRect(startX, startY, startX + sectionLength, startY + offY, 0x1AFF0000);
                }

                x++;

                if (x > 2) {
                    y++;
                    x = 0;
                }
            }
        }

        if (this.tooltip >= 0 && dspToolTip.length() > 0) {
            this.drawTooltip(toolPosX, toolPosY + 10, 0, dspToolTip);
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.setScrollBar();
        this.bindTexture("guis/craftingreport.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    private void setScrollBar() {
        final int size = this.visual.size();

        this.getScrollBar().setTop(19).setLeft(218).setHeight(114);
        this.getScrollBar().setRange(0, (size + 2) / 3 - this.rows, 1);
    }

    public void postUpdate(final List<IAEItemStack> list, final byte ref) {
        switch (ref) {
            case 0:
                for (final IAEItemStack l : list) {
                    this.handleInput(this.storage, l);
                }
                break;

            case 1:
                for (final IAEItemStack l : list) {
                    this.handleInput(this.pending, l);
                }
                break;

            case 2:
                for (final IAEItemStack l : list) {
                    this.handleInput(this.missing, l);
                }
                break;
        }

        for (final IAEItemStack l : list) {
            final long amt = this.getTotal(l);

            if (amt <= 0) {
                this.deleteVisualStack(l);
            } else {
                final IAEItemStack is = this.findVisualStack(l);
                is.setStackSize(amt);
            }
        }
        this.sortItems();
        this.setScrollBar();
    }

    Comparator<IAEItemStack> comparator = (i1, i2) -> {
        if (missing.findPrecise(i1) != null) {
            if (missing.findPrecise(i2) != null) return 0;
            return -1;
        } else if (missing.findPrecise(i2) != null) {
            return 1;
        } else {
            return 0;
        }
    };

    private void sortItems() {
        if (!this.missing.isEmpty()) {
            this.visual.sort(comparator);
        }
    }

    private void handleInput(final IItemList<IAEItemStack> s, final IAEItemStack l) {
        IAEItemStack a = s.findPrecise(l);

        if (l.getStackSize() <= 0) {
            if (a != null) {
                a.reset();
            }
        } else {
            if (a == null) {
                s.add(l.copy());
                a = s.findPrecise(l);
            }

            if (a != null) {
                a.setStackSize(l.getStackSize());
            }
        }
    }

    private long getTotal(final IAEItemStack is) {
        final IAEItemStack a = this.storage.findPrecise(is);
        final IAEItemStack c = this.pending.findPrecise(is);
        final IAEItemStack m = this.missing.findPrecise(is);

        long total = 0;

        if (a != null) {
            total += a.getStackSize();
        }

        if (c != null) {
            total += c.getStackSize();
        }

        if (m != null) {
            total += m.getStackSize();
        }

        return total;
    }

    private void deleteVisualStack(final IAEItemStack l) {
        final Iterator<IAEItemStack> i = this.visual.iterator();
        while (i.hasNext()) {
            final IAEItemStack o = i.next();
            if (o.equals(l)) {
                i.remove();
                return;
            }
        }
    }

    private IAEItemStack findVisualStack(final IAEItemStack l) {
        for (final IAEItemStack o : this.visual) {
            if (o.equals(l)) {
                return o;
            }
        }

        final IAEItemStack stack = l.copy();
        this.visual.add(stack);
        return stack;
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == 28) {
                this.actionPerformed(this.start);
            }
            super.keyTyped(character, key);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        final boolean backwards = Mouse.isButtonDown(1);

        if (btn == this.selectCPU) {
            FluidCraft.proxy.netHandler
                    .sendToServer(new CPacketFluidPatternTermBtns("Terminal.Cpu", backwards ? "Prev" : "Next"));
        }

        if (btn == this.cancel) {
            this.addMissingItemsToBookMark();
            FluidCraft.proxy.netHandler.sendToServer(new CPacketSwitchGuis(this.OriginalGui));
        }

        if (btn == this.start) {
            try {
                FluidCraft.proxy.netHandler.sendToServer(new CPacketFluidPatternTermBtns("Terminal.Start", "Start"));
            } catch (final Throwable e) {
                AELog.debug(e);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void addItemTooltip(ItemStack is, List<String> lineList) {
        if (isShiftKeyDown()) {
            List l = is.getTooltip(this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips);
            if (!l.isEmpty()) l.remove(0);
            lineList.addAll(l);
        } else if (ModAndClassUtil.isShiftTooltip) {
            lineList.add(GuiText.HoldShiftForTooltip.getLocal());
        }
    }

    public ItemStack getHoveredStack() {
        return hoveredStack;
    }

    protected void addMissingItemsToBookMark() {
        if (!this.missing.isEmpty() && isShiftKeyDown()) {
            for (IAEItemStack iaeItemStack : this.missing) {
                NEI.instance.addItemToBookMark(iaeItemStack.getItemStack());
            }
        }
    }

    public IItemList<IAEItemStack> getStorage() {
        return this.storage;
    }

    public IItemList<IAEItemStack> getPending() {
        return this.pending;
    }

    public IItemList<IAEItemStack> getMissing() {
        return this.missing;
    }
}
