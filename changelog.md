v0.2.7-beta | SAVE COMPATIBLE
- Added a new policy reducing drugs demand
- Rebalanced the policies a bit
- Added a new event
- Fixed the price function
- Added a new stockpile icon to indicate no demand
- Farming is now more labor intensive

v0.2.6-beta | NOT SAVE COMPATIBLE
- Added LunaLib settings support
- Added a table to the command tab colonies section to show faction controlled resources.
- UI fixes
- Fixed best places to sell & buy list.
- Fixed exotic goods having strange prices.

v0.2.5-beta | SAVE COMPATIBLE
- Changed some dialog background transparency
- Overhauled the worker assigner. The workers should now be distributed more efficiently, reducing deficits.
- Misc. bug fixes

v0.2.4-beta | SAVE COMPATIBLE
- Fixed colony income tooltip not being expandable for more info
- Fixed policies list not being scrollable
- Fixed policy cooldown icon not showing
- Fixed industry build / upgrade state not having a darker image.
- Fixed critical bug in colony income redistribution
- Tuned down the policy Bulwark of Orichalcum
- Fixed a bug causing upgradable industries with new outputs from being visible.
- Manufacturing no longer interferes with luddic majority buff
- Added a switch button to Colonies tab that switches to colony population statistics table
- Ctrl + Clicking the Commodity Selection Panel under the Economy section inside the command tab will open the global market info panel.
- Added the infrastructure for colony events
- Fixed check for manufacturing industry to markets.
- Made light industry more labor intensive

v0.2.3-beta | SAVE COMPATIBLE
- Modified UI code to fit the new WrapUI changes of composition.
- Fixed crash where the tooltip was trying to access removed industries.
- The Commodity tooltip now shows the correct best places to buy/sell tables.

v0.2.2-beta | SAVE COMPATIBLE
- Redistributing credits between faction markets should no longer create more credits than existing
- Top 5 producers/consumers table can now be Ctrl + Clicked to set a target to the market
- More bug fixes
- A stockpiles icon was added to Commodity Panel inside Market Details section.
- Updated the tooltip on Commodity Panel to make seperation between daily flows and stockpiles clearer
- Fixed orbital station mid and high configs. Values should now reflect intended amounts

v0.2.1-beta | SAVE COMPATIBLE
- Changed dynamic industry config creation to be less intrusive. This should decrease crashes.
- More bug fixes
- Changed location of config files

v0.2.0-beta | NOT SAVE COMPATIBLE
- Switched to RolflectionLib to stop relying on kotlin
- Switched to WrapUI Dialogs for more consistent behaviour
- Bug fixes
- The sector now starts with stockpiles to prevent death spirals.
- Larger markets now have a smaller ratio of their population as workers, while smaller markets more.
- The player can now embargo other factions at a reputation cost
- Added more policies
- Added flavor bar events dependent on colony policies
- Population Health and Happiness now have direct effects on the colony

v0.1.0-beta
- Initial release