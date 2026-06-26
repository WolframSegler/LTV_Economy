# LTV-Economy

A deep overhaul of Starsector's economy and additional colony systems.
LTV-Economy replaces abstract production values with a **unit-based simulation** where production and trade correspond to actual cargo units. The mod deepens economic simulation by introducing **workers** as a production factor. Player-enacted policies and market events build on top of this framework. Trade and patrol fleets are deeply integrated into the economy, corresponding to concrete trade missions, and assembled from the faction ship inventory.

[![Download](https://img.shields.io/badge/Download-Latest%20Release-brightgreen)](https://github.com/WolframSegler/LTV_Economy/releases/latest/download/LTV_Economy.zip)


## Forum Page
Content details related to the mod can be found on [the fractalsoftworks forums](https://fractalsoftworks.com/forum/index.php?topic=34632).


## Incompatible Mods

- <a href="https://fractalsoftworks.com/forum/index.php?topic=20986.0">Grand.Colonies</a>
- <a href="https://fractalsoftworks.com/forum/index.php?topic=28273.0">Astral Ascension</a>
- <a href="https://fractalsoftworks.com/forum/index.php?topic=26307.0">Ashes of The Domain</a>
- <a href="https://fractalsoftworks.com/forum/index.php?topic=26969.0">Starpocalypse Revengeance</a>


## For Other Modders

- For full compatibility, add your own industry configs to `data/config/ltvEcon/industry_config.json`. See [`industry_config_doc.md`](data/config/ltvEcon/industry_config_doc.md) for details.
- To add market policies: place a `policy_config.json` file inside `data/config/ltvEcon/` in your own mod folder.
- To add market events: place an `events_config.json` file inside `data/config/ltvEcon/` in your own mod folder.
- This mod automatically removes the following submarket from markets: `local_resources`.
- This mod adds the following submarket: `stockpiles`. It is a view to the CommodityCell's of a market.
- This mod overrides the following submarkets: `open_market`, `black_market`, `generic_military`. This is done to modify their stockpile limits.
- This mod overrides the following industries: `patrolhq`, `militarybase`, `highcommand`.

## Credits

Lukas04 -  for providing the access pattern to UI Panels inside the colony detail screen.

SirHartley - for helping with IndustryOptionProvider.

rolfosian - for providing the [reflection library](https://github.com/rolfosian/RolflectionLib-SS).

Kaysaar - for advice on UI design, creative direction and reflection help.

fgdfgfthgr-fox - for their relentless work providing bug reports and helping me polish the mod.

<br>

## Attributes

<a href="https://www.flaticon.com/free-icons/measurements" title="measurements icons">Measurements icons created by Vectors Tank - Flaticon</a>

<a href="https://www.shutterstock.com/image-vector/construction-helmet-icon-simple-element-collection-2502692379?trackingId=018e5ba4-d525-4580-9880-d5b5ff10a345" title="construction helmet">Construction Helmet</a>

<a target="_blank" href="https://icons8.com/icon/6kkYMKzmRC9P/solidarity">Solidarity</a> Icon by <a target="_blank" href="https://icons8.com">Icons8</a>

<a target="_blank" href="https://icons8.com/icon/sm20ePRz8ZPO/solidarity">Solidarity</a> Icon by <a target="_blank" href="https://icons8.com">Icons8</a>

<a href="https://www.freepik.com/icon/community_15329516">Icon by Nuricon</a>

<a href="https://www.freepik.com/icon/happy_2441887">Icon by Freepik</a>

<a href="https://www.freepik.com/premium-vector/beautiful-medical-healthcare-heart-icon-vector-illustration_358640287.htm">Heart</a>

<a target="_blank" href="https://icons8.com/icon/eXZFp2e0w8EX/no-entry">Restricted</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>

<a target="_blank" href="https://icons8.com/icon/9028/automatic">Settings</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>

Pallet by Cherry from <a href="https://thenounproject.com/browse/icons/term/pallet/" target="_blank" title="Pallet Icons">Noun Project</a> (CC BY 3.0)

behaviour by Adrien Coquet from <a href="https://thenounproject.com/browse/icons/term/behaviour/" target="_blank" title="behaviour Icons">Noun Project</a> (CC BY 3.0)

<a href="https://www.flaticon.com/free-icons/next" title="next icons">Next icons created by Roundicons - Flaticon</a>

<a href="https://www.flaticon.com/free-icons/dormitory" title="dormitory icons">Dormitory icons created by Satawat Anukul - Flaticon</a>

<a target="_blank" href="https://icons8.com/icon/124202/test-passed">Checklist</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>

<a target="_blank" href="https://icons8.com/icon/tLYYFUAkhhRa/stopwatch">Stopwatch</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>

Healthcare by Pong Pong from <a href="https://thenounproject.com/browse/icons/term/healthcare/" target="_blank" title="Healthcare Icons">Noun Project</a> (CC BY 3.0)

Radio Tower by Rockicon from <a href="https://thenounproject.com/browse/icons/term/radio-tower/" target="_blank" title="Radio Tower Icons">Noun Project</a> (CC BY 3.0)

theater mask by Hilmy Abiyyu Asad from <a href="https://thenounproject.com/browse/icons/term/theater-mask/" target="_blank" title="theater mask Icons">Noun Project</a> (CC BY 3.0)

customize by Adrien Coquet from <a href="https://thenounproject.com/browse/icons/term/customize/" target="_blank" title="customize Icons">Noun Project</a> (CC BY 3.0)

Blueprint by NAPISAH from <a href="https://thenounproject.com/browse/icons/term/blueprint/" target="_blank" title="Blueprint Icons">Noun Project</a> (CC BY 3.0)

Forbidden by Kosong Tujuh from <a href="https://thenounproject.com/browse/icons/term/forbidden/" target="_blank" title="Forbidden Icons">Noun Project</a> (CC BY 3.0)

The Fairest of the Fair by <a href="https://www.youtube.com/@usnavyband" target="_blank" title="Forbidden Icons">United States Navy Band</a>

<br>

## AI Generated Assets

<ul>
    <li>graphics/events/miasma.png</li>
    <li>graphics/events/gang_violence.png</li>
    <li>graphics/policies/extended_shifts.png</li>
    <li>graphics/policies/luddic_prophet_festival.png</li>
    <li>graphics/policies/bulwark_of_orichalcum.png</li>
    <li>graphics/policies/default_policy_poster.png</li>
    <li>graphics/policies/fleet_mobilization_order.png</li>
    <li>graphics/policies/iron_fist_decree.png</li>
    <li>graphics/policies/organ_harvesting_program.png</li>
    <li>graphics/policies/pharmaceutical_promotion.png</li>
    <li>graphics/policies/urban_renewal_initiative.png</li>
    <li>graphics/policies/public_health_education.png</li>
    <li>graphics/policies/bres_vitalis.png</li>
    <li>graphics/policies/convergence_festival.png</li>
    <li>graphics/policies/substance_control_act.png</li>
    <li>graphics/policies/expand_the_yards.png</li>
    <li>graphics/icons/markets/wfg_labor.png</li>
    <li>graphics/icons/intel/policy_icon.png</li>
    <li>graphics/icons/industry/manufacturing.png</li>
    <li>graphics/icons/cargo/components_light_1.png</li>
    <li>graphics/icons/cargo/components_light_2.png</li>
    <li>graphics/icons/cargo/components_light_3.png</li>
    <li>graphics/icons/cargo/components_precision_1.png</li>
    <li>graphics/icons/cargo/components_structural_1.png</li>
</ul>