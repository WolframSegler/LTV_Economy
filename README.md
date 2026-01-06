# LTV_Economy

A deep overhaul of Starsector’s economic and colony systems.  
LTV_Economy replaces abstract income with a unit-based simulation where production, trade, labor, and policy decisions all interact in concrete, traceable ways.

This mod is designed to make the economy **interesting, legible, and consequential** without turning it into a spreadsheet nightmare.

[![Download](https://img.shields.io/badge/Download-Latest%20Release-brightgreen)](https://github.com/WolframSegler/LTV_Economy/releases/latest/download/LTV_Economy.zip)

<br>

## Core Features

### Unit-based economy
- Industries **consume and produce real commodity units**.
- Trade moves actual units between markets; credits are derived from prices × quantities.
- All flows (production, imports, exports, deficits, storage) are tracked explicitly.

### CommodityStats
- Central system that tracks:
  - Production, demand, imports (faction/global), exports
  - Deficits, over-imports, storage, stockpiles
- Provides consistent, debuggable economic behavior across all markets.

### Labor & wages
- Workers are assigned to **outputs**, not just industries.
- Production scales with assigned labor.
- Workers are paid wages, which are real upkeep costs.
- Vanilla modifiers are translated into LTV-relevant values.

### Industry IO system
- Every industry has explicit inputs and outputs.
- Missing IO definitions are generated dynamically.
- New **Manufacturing** industry acts as a labor-heavy intermediary between raw extraction and heavy/light industry.

<br>

## Trade & AI Behavior

- Markets trade using a **pairing-score system** ti determine trade order.
- Non-player markets assign workers via a **two-stage solver**:
  1. Compute global demand and required labor per output.
  2. Distribute labor fairly across markets with capacity constraints.

<br>

## Population Simulation

Introduces a population model with four tracked values:
- **Health**
- **Happiness**
- **Social Cohesion**
- **Class Consciousness**

These values currently influence colony behavior indirectly and will gain stronger gameplay effects in future updates.  
They respond to wages, stability, exploitation, and policies.

<br>

## Credits, Debt & Policy

### Market credits & debt
- Markets can go into **debt**.
- Debt does *not* hard-stop trade, preventing economic soft-locks.
- Debt applies tiered penalties:
  - Stability loss
  - Increased upkeep
  - Reduced immigration
  - Reduced extractable player income

### Player income control
- Colonies have a **Player Profit Ratio** (auto-transfer).
- Only real surplus can be extracted; in-debt colonies cannot lend money.

<br>

<br>

## Modding & Compatibility

- Designed to be **data-driven and extensible**.
- Clear separation between simulation logic and UI.
- WrapUI is reusable and intended for other mods.
- Compatible with most mods that do not deeply replace the economy.

<br>

## Scope & Status

- Over **8 months of development** and hundreds of hours of work.
- Balance is intentionally conservative and will be refined through player feedback.

<br>

## Compatibility

Due to the intrusive nature of the mod, anything that modifies the Colony Info Panel are likely to cause problems. Here are mods that are definitely incompatible:
- <a href="https://fractalsoftworks.com/forum/index.php?topic=20986.0">Grand.Colonies</a>

<br>

## For Other Modders

If you want your mod's industries to be compatible, you can add an <code>industry_config.json</code> entry under <code>data/config/</code> inside your own mod folder. The documentation file <code>data/config/industry_config_doc.md</code> provides the necessary information.

<br><br>

## Roadmap
- Add more policies
- Add more market events as a consequence of the population
- LunaLib support

<br><br>

## Credits

Lukas04 - for providing ReflectionUtils and providing the correct hirearchy for accessing certain classes under LtvMarketReplacer.

SirHartley - for helping with IndustryOptionProvider

rolfosian - for RolflectionLib-SS

<br>

## Attributes

<a href="https://www.flaticon.com/free-icons/business-and-finance" title="business and finance icons">Business and finance icons created by Paul J. - Flaticon</a>

<a href="https://www.flaticon.com/free-icons/measurements" title="measurements icons">Measurements icons created by Vectors Tank - Flaticon</a>

<a href="https://www.shutterstock.com/image-vector/construction-helmet-icon-simple-element-collection-2502692379?trackingId=018e5ba4-d525-4580-9880-d5b5ff10a345" title="construction helmet">Construction Helmet</a>

<a target="_blank" href="https://icons8.com/icon/6kkYMKzmRC9P/solidarity">Solidarität</a> Icon von <a target="_blank" href="https://icons8.com">Icons8</a>

<a target="_blank" href="https://icons8.com/icon/sm20ePRz8ZPO/solidarity">Solidarität</a> Icon von <a target="_blank" href="https://icons8.com">Icons8</a>

<a href="https://www.freepik.com/icon/community_15329516">Icon by Nuricon</a>

<a href="https://www.freepik.com/icon/happy_2441887">Icon by Freepik</a>

<a href="https://www.freepik.com/premium-vector/beautiful-medical-healthcare-heart-icon-vector-illustration_358640287.htm">Heart</a>

<a target="_blank" href="https://icons8.com/icon/eXZFp2e0w8EX/no-entry">Restricted</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>

<br>

## AI Generated Assets

<ul>
    <li>graphics/policies/extended_shifts.png</li>
    <li>graphics/policies/luddic_prophet_festival.png</li>
    <li>graphics/icons/markets/wfg_labor.png</li>
    <li>graphics/icons/intel/policy_icon.png</li>
    <li>graphics/icons/industry/manufacturing.png</li>
    <li>graphics/icons/cargo/components_light_1.png</li>
    <li>graphics/icons/cargo/components_light_2.png</li>
    <li>graphics/icons/cargo/components_light_3.png</li>
    <li>graphics/icons/cargo/components_precision_1.png</li>
    <li>graphics/icons/cargo/components_structural_1.png</li>
    <li>graphics/icons/cargo/components_subassembly_1.png</li>
    <li>graphics/icons/cargo/components_subassembly_2.png</li>
    <li>graphics/icons/cargo/components_subassembly_3.png</li>
    <li>graphics/icons/cargo/light_machinery_1.png</li>
</ul>