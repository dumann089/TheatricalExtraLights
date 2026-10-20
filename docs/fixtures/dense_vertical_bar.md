# Dense Vertical RGB Bar

`theatricalextralights:dense_vertical_bar` · family: [PARs & LED panels](/fixtures/pars-panels)

The Vertical RGB Bar with forty-six pixels, one every sixteenth of a block.

## Personalities

| Mode | Channels |
|---|---|
| 4-Channel Mode | 4 |
| 184-Channel Pixel Mode (46x Dim/RGB) | 184 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 4-Channel Mode (4 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

## 184-Channel Pixel Mode (46x Dim/RGB) (184 ch)

Pixels are numbered 1 to 46 from the bottom to the top of the bar as it stands. Each pixel lights its cell and its LED dot; a single lit pixel throws one thin sheet of light into the haze, and neighbouring lit pixels of the same colour merge into one sheet that widens with them. There is no master dimmer: to fade the whole bar, select the fixture and use its dimmer. A ready-made [grandMA2 fixture file](/guide/grandma2) exists for this mode, with the pixels as sub-fixtures 1.1 to 1.46.

| Ch | Function | Values |
|---|---|---|
| 1 | Pixel 1 dimmer | 0 off to 255 full |
| 2 | Pixel 1 red | 0 to 255 |
| 3 | Pixel 1 green | 0 to 255 |
| 4 | Pixel 1 blue | 0 to 255 |
| 5 | Pixel 2 dimmer | 0 off to 255 full |
| 6 | Pixel 2 red | 0 to 255 |
| 7 | Pixel 2 green | 0 to 255 |
| 8 | Pixel 2 blue | 0 to 255 |
| 9 | Pixel 3 dimmer | 0 off to 255 full |
| 10 | Pixel 3 red | 0 to 255 |
| 11 | Pixel 3 green | 0 to 255 |
| 12 | Pixel 3 blue | 0 to 255 |
| 13 | Pixel 4 dimmer | 0 off to 255 full |
| 14 | Pixel 4 red | 0 to 255 |
| 15 | Pixel 4 green | 0 to 255 |
| 16 | Pixel 4 blue | 0 to 255 |
| 17 | Pixel 5 dimmer | 0 off to 255 full |
| 18 | Pixel 5 red | 0 to 255 |
| 19 | Pixel 5 green | 0 to 255 |
| 20 | Pixel 5 blue | 0 to 255 |
| 21 | Pixel 6 dimmer | 0 off to 255 full |
| 22 | Pixel 6 red | 0 to 255 |
| 23 | Pixel 6 green | 0 to 255 |
| 24 | Pixel 6 blue | 0 to 255 |
| 25 | Pixel 7 dimmer | 0 off to 255 full |
| 26 | Pixel 7 red | 0 to 255 |
| 27 | Pixel 7 green | 0 to 255 |
| 28 | Pixel 7 blue | 0 to 255 |
| 29 | Pixel 8 dimmer | 0 off to 255 full |
| 30 | Pixel 8 red | 0 to 255 |
| 31 | Pixel 8 green | 0 to 255 |
| 32 | Pixel 8 blue | 0 to 255 |
| 33 | Pixel 9 dimmer | 0 off to 255 full |
| 34 | Pixel 9 red | 0 to 255 |
| 35 | Pixel 9 green | 0 to 255 |
| 36 | Pixel 9 blue | 0 to 255 |
| 37 | Pixel 10 dimmer | 0 off to 255 full |
| 38 | Pixel 10 red | 0 to 255 |
| 39 | Pixel 10 green | 0 to 255 |
| 40 | Pixel 10 blue | 0 to 255 |
| 41 | Pixel 11 dimmer | 0 off to 255 full |
| 42 | Pixel 11 red | 0 to 255 |
| 43 | Pixel 11 green | 0 to 255 |
| 44 | Pixel 11 blue | 0 to 255 |
| 45 | Pixel 12 dimmer | 0 off to 255 full |
| 46 | Pixel 12 red | 0 to 255 |
| 47 | Pixel 12 green | 0 to 255 |
| 48 | Pixel 12 blue | 0 to 255 |
| 49 | Pixel 13 dimmer | 0 off to 255 full |
| 50 | Pixel 13 red | 0 to 255 |
| 51 | Pixel 13 green | 0 to 255 |
| 52 | Pixel 13 blue | 0 to 255 |
| 53 | Pixel 14 dimmer | 0 off to 255 full |
| 54 | Pixel 14 red | 0 to 255 |
| 55 | Pixel 14 green | 0 to 255 |
| 56 | Pixel 14 blue | 0 to 255 |
| 57 | Pixel 15 dimmer | 0 off to 255 full |
| 58 | Pixel 15 red | 0 to 255 |
| 59 | Pixel 15 green | 0 to 255 |
| 60 | Pixel 15 blue | 0 to 255 |
| 61 | Pixel 16 dimmer | 0 off to 255 full |
| 62 | Pixel 16 red | 0 to 255 |
| 63 | Pixel 16 green | 0 to 255 |
| 64 | Pixel 16 blue | 0 to 255 |
| 65 | Pixel 17 dimmer | 0 off to 255 full |
| 66 | Pixel 17 red | 0 to 255 |
| 67 | Pixel 17 green | 0 to 255 |
| 68 | Pixel 17 blue | 0 to 255 |
| 69 | Pixel 18 dimmer | 0 off to 255 full |
| 70 | Pixel 18 red | 0 to 255 |
| 71 | Pixel 18 green | 0 to 255 |
| 72 | Pixel 18 blue | 0 to 255 |
| 73 | Pixel 19 dimmer | 0 off to 255 full |
| 74 | Pixel 19 red | 0 to 255 |
| 75 | Pixel 19 green | 0 to 255 |
| 76 | Pixel 19 blue | 0 to 255 |
| 77 | Pixel 20 dimmer | 0 off to 255 full |
| 78 | Pixel 20 red | 0 to 255 |
| 79 | Pixel 20 green | 0 to 255 |
| 80 | Pixel 20 blue | 0 to 255 |
| 81 | Pixel 21 dimmer | 0 off to 255 full |
| 82 | Pixel 21 red | 0 to 255 |
| 83 | Pixel 21 green | 0 to 255 |
| 84 | Pixel 21 blue | 0 to 255 |
| 85 | Pixel 22 dimmer | 0 off to 255 full |
| 86 | Pixel 22 red | 0 to 255 |
| 87 | Pixel 22 green | 0 to 255 |
| 88 | Pixel 22 blue | 0 to 255 |
| 89 | Pixel 23 dimmer | 0 off to 255 full |
| 90 | Pixel 23 red | 0 to 255 |
| 91 | Pixel 23 green | 0 to 255 |
| 92 | Pixel 23 blue | 0 to 255 |
| 93 | Pixel 24 dimmer | 0 off to 255 full |
| 94 | Pixel 24 red | 0 to 255 |
| 95 | Pixel 24 green | 0 to 255 |
| 96 | Pixel 24 blue | 0 to 255 |
| 97 | Pixel 25 dimmer | 0 off to 255 full |
| 98 | Pixel 25 red | 0 to 255 |
| 99 | Pixel 25 green | 0 to 255 |
| 100 | Pixel 25 blue | 0 to 255 |
| 101 | Pixel 26 dimmer | 0 off to 255 full |
| 102 | Pixel 26 red | 0 to 255 |
| 103 | Pixel 26 green | 0 to 255 |
| 104 | Pixel 26 blue | 0 to 255 |
| 105 | Pixel 27 dimmer | 0 off to 255 full |
| 106 | Pixel 27 red | 0 to 255 |
| 107 | Pixel 27 green | 0 to 255 |
| 108 | Pixel 27 blue | 0 to 255 |
| 109 | Pixel 28 dimmer | 0 off to 255 full |
| 110 | Pixel 28 red | 0 to 255 |
| 111 | Pixel 28 green | 0 to 255 |
| 112 | Pixel 28 blue | 0 to 255 |
| 113 | Pixel 29 dimmer | 0 off to 255 full |
| 114 | Pixel 29 red | 0 to 255 |
| 115 | Pixel 29 green | 0 to 255 |
| 116 | Pixel 29 blue | 0 to 255 |
| 117 | Pixel 30 dimmer | 0 off to 255 full |
| 118 | Pixel 30 red | 0 to 255 |
| 119 | Pixel 30 green | 0 to 255 |
| 120 | Pixel 30 blue | 0 to 255 |
| 121 | Pixel 31 dimmer | 0 off to 255 full |
| 122 | Pixel 31 red | 0 to 255 |
| 123 | Pixel 31 green | 0 to 255 |
| 124 | Pixel 31 blue | 0 to 255 |
| 125 | Pixel 32 dimmer | 0 off to 255 full |
| 126 | Pixel 32 red | 0 to 255 |
| 127 | Pixel 32 green | 0 to 255 |
| 128 | Pixel 32 blue | 0 to 255 |
| 129 | Pixel 33 dimmer | 0 off to 255 full |
| 130 | Pixel 33 red | 0 to 255 |
| 131 | Pixel 33 green | 0 to 255 |
| 132 | Pixel 33 blue | 0 to 255 |
| 133 | Pixel 34 dimmer | 0 off to 255 full |
| 134 | Pixel 34 red | 0 to 255 |
| 135 | Pixel 34 green | 0 to 255 |
| 136 | Pixel 34 blue | 0 to 255 |
| 137 | Pixel 35 dimmer | 0 off to 255 full |
| 138 | Pixel 35 red | 0 to 255 |
| 139 | Pixel 35 green | 0 to 255 |
| 140 | Pixel 35 blue | 0 to 255 |
| 141 | Pixel 36 dimmer | 0 off to 255 full |
| 142 | Pixel 36 red | 0 to 255 |
| 143 | Pixel 36 green | 0 to 255 |
| 144 | Pixel 36 blue | 0 to 255 |
| 145 | Pixel 37 dimmer | 0 off to 255 full |
| 146 | Pixel 37 red | 0 to 255 |
| 147 | Pixel 37 green | 0 to 255 |
| 148 | Pixel 37 blue | 0 to 255 |
| 149 | Pixel 38 dimmer | 0 off to 255 full |
| 150 | Pixel 38 red | 0 to 255 |
| 151 | Pixel 38 green | 0 to 255 |
| 152 | Pixel 38 blue | 0 to 255 |
| 153 | Pixel 39 dimmer | 0 off to 255 full |
| 154 | Pixel 39 red | 0 to 255 |
| 155 | Pixel 39 green | 0 to 255 |
| 156 | Pixel 39 blue | 0 to 255 |
| 157 | Pixel 40 dimmer | 0 off to 255 full |
| 158 | Pixel 40 red | 0 to 255 |
| 159 | Pixel 40 green | 0 to 255 |
| 160 | Pixel 40 blue | 0 to 255 |
| 161 | Pixel 41 dimmer | 0 off to 255 full |
| 162 | Pixel 41 red | 0 to 255 |
| 163 | Pixel 41 green | 0 to 255 |
| 164 | Pixel 41 blue | 0 to 255 |
| 165 | Pixel 42 dimmer | 0 off to 255 full |
| 166 | Pixel 42 red | 0 to 255 |
| 167 | Pixel 42 green | 0 to 255 |
| 168 | Pixel 42 blue | 0 to 255 |
| 169 | Pixel 43 dimmer | 0 off to 255 full |
| 170 | Pixel 43 red | 0 to 255 |
| 171 | Pixel 43 green | 0 to 255 |
| 172 | Pixel 43 blue | 0 to 255 |
| 173 | Pixel 44 dimmer | 0 off to 255 full |
| 174 | Pixel 44 red | 0 to 255 |
| 175 | Pixel 44 green | 0 to 255 |
| 176 | Pixel 44 blue | 0 to 255 |
| 177 | Pixel 45 dimmer | 0 off to 255 full |
| 178 | Pixel 45 red | 0 to 255 |
| 179 | Pixel 45 green | 0 to 255 |
| 180 | Pixel 45 blue | 0 to 255 |
| 181 | Pixel 46 dimmer | 0 off to 255 full |
| 182 | Pixel 46 red | 0 to 255 |
| 183 | Pixel 46 green | 0 to 255 |
| 184 | Pixel 46 blue | 0 to 255 |

### Channel by channel

**1 · Pixel 1 dimmer** — Dimmer of pixel 1. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**2 · Pixel 1 red** — Red of pixel 1.

**3 · Pixel 1 green** — Green of pixel 1.

**4 · Pixel 1 blue** — Blue of pixel 1.

**5 · Pixel 2 dimmer** — Dimmer of pixel 2. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**6 · Pixel 2 red** — Red of pixel 2.

**7 · Pixel 2 green** — Green of pixel 2.

**8 · Pixel 2 blue** — Blue of pixel 2.

**9 · Pixel 3 dimmer** — Dimmer of pixel 3. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**10 · Pixel 3 red** — Red of pixel 3.

**11 · Pixel 3 green** — Green of pixel 3.

**12 · Pixel 3 blue** — Blue of pixel 3.

**13 · Pixel 4 dimmer** — Dimmer of pixel 4. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**14 · Pixel 4 red** — Red of pixel 4.

**15 · Pixel 4 green** — Green of pixel 4.

**16 · Pixel 4 blue** — Blue of pixel 4.

**17 · Pixel 5 dimmer** — Dimmer of pixel 5. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**18 · Pixel 5 red** — Red of pixel 5.

**19 · Pixel 5 green** — Green of pixel 5.

**20 · Pixel 5 blue** — Blue of pixel 5.

**21 · Pixel 6 dimmer** — Dimmer of pixel 6. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**22 · Pixel 6 red** — Red of pixel 6.

**23 · Pixel 6 green** — Green of pixel 6.

**24 · Pixel 6 blue** — Blue of pixel 6.

**25 · Pixel 7 dimmer** — Dimmer of pixel 7. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**26 · Pixel 7 red** — Red of pixel 7.

**27 · Pixel 7 green** — Green of pixel 7.

**28 · Pixel 7 blue** — Blue of pixel 7.

**29 · Pixel 8 dimmer** — Dimmer of pixel 8. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**30 · Pixel 8 red** — Red of pixel 8.

**31 · Pixel 8 green** — Green of pixel 8.

**32 · Pixel 8 blue** — Blue of pixel 8.

**33 · Pixel 9 dimmer** — Dimmer of pixel 9. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**34 · Pixel 9 red** — Red of pixel 9.

**35 · Pixel 9 green** — Green of pixel 9.

**36 · Pixel 9 blue** — Blue of pixel 9.

**37 · Pixel 10 dimmer** — Dimmer of pixel 10. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**38 · Pixel 10 red** — Red of pixel 10.

**39 · Pixel 10 green** — Green of pixel 10.

**40 · Pixel 10 blue** — Blue of pixel 10.

**41 · Pixel 11 dimmer** — Dimmer of pixel 11. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**42 · Pixel 11 red** — Red of pixel 11.

**43 · Pixel 11 green** — Green of pixel 11.

**44 · Pixel 11 blue** — Blue of pixel 11.

**45 · Pixel 12 dimmer** — Dimmer of pixel 12. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**46 · Pixel 12 red** — Red of pixel 12.

**47 · Pixel 12 green** — Green of pixel 12.

**48 · Pixel 12 blue** — Blue of pixel 12.

**49 · Pixel 13 dimmer** — Dimmer of pixel 13. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**50 · Pixel 13 red** — Red of pixel 13.

**51 · Pixel 13 green** — Green of pixel 13.

**52 · Pixel 13 blue** — Blue of pixel 13.

**53 · Pixel 14 dimmer** — Dimmer of pixel 14. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**54 · Pixel 14 red** — Red of pixel 14.

**55 · Pixel 14 green** — Green of pixel 14.

**56 · Pixel 14 blue** — Blue of pixel 14.

**57 · Pixel 15 dimmer** — Dimmer of pixel 15. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**58 · Pixel 15 red** — Red of pixel 15.

**59 · Pixel 15 green** — Green of pixel 15.

**60 · Pixel 15 blue** — Blue of pixel 15.

**61 · Pixel 16 dimmer** — Dimmer of pixel 16. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**62 · Pixel 16 red** — Red of pixel 16.

**63 · Pixel 16 green** — Green of pixel 16.

**64 · Pixel 16 blue** — Blue of pixel 16.

**65 · Pixel 17 dimmer** — Dimmer of pixel 17. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**66 · Pixel 17 red** — Red of pixel 17.

**67 · Pixel 17 green** — Green of pixel 17.

**68 · Pixel 17 blue** — Blue of pixel 17.

**69 · Pixel 18 dimmer** — Dimmer of pixel 18. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**70 · Pixel 18 red** — Red of pixel 18.

**71 · Pixel 18 green** — Green of pixel 18.

**72 · Pixel 18 blue** — Blue of pixel 18.

**73 · Pixel 19 dimmer** — Dimmer of pixel 19. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**74 · Pixel 19 red** — Red of pixel 19.

**75 · Pixel 19 green** — Green of pixel 19.

**76 · Pixel 19 blue** — Blue of pixel 19.

**77 · Pixel 20 dimmer** — Dimmer of pixel 20. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**78 · Pixel 20 red** — Red of pixel 20.

**79 · Pixel 20 green** — Green of pixel 20.

**80 · Pixel 20 blue** — Blue of pixel 20.

**81 · Pixel 21 dimmer** — Dimmer of pixel 21. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**82 · Pixel 21 red** — Red of pixel 21.

**83 · Pixel 21 green** — Green of pixel 21.

**84 · Pixel 21 blue** — Blue of pixel 21.

**85 · Pixel 22 dimmer** — Dimmer of pixel 22. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**86 · Pixel 22 red** — Red of pixel 22.

**87 · Pixel 22 green** — Green of pixel 22.

**88 · Pixel 22 blue** — Blue of pixel 22.

**89 · Pixel 23 dimmer** — Dimmer of pixel 23. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**90 · Pixel 23 red** — Red of pixel 23.

**91 · Pixel 23 green** — Green of pixel 23.

**92 · Pixel 23 blue** — Blue of pixel 23.

**93 · Pixel 24 dimmer** — Dimmer of pixel 24. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**94 · Pixel 24 red** — Red of pixel 24.

**95 · Pixel 24 green** — Green of pixel 24.

**96 · Pixel 24 blue** — Blue of pixel 24.

**97 · Pixel 25 dimmer** — Dimmer of pixel 25. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**98 · Pixel 25 red** — Red of pixel 25.

**99 · Pixel 25 green** — Green of pixel 25.

**100 · Pixel 25 blue** — Blue of pixel 25.

**101 · Pixel 26 dimmer** — Dimmer of pixel 26. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**102 · Pixel 26 red** — Red of pixel 26.

**103 · Pixel 26 green** — Green of pixel 26.

**104 · Pixel 26 blue** — Blue of pixel 26.

**105 · Pixel 27 dimmer** — Dimmer of pixel 27. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**106 · Pixel 27 red** — Red of pixel 27.

**107 · Pixel 27 green** — Green of pixel 27.

**108 · Pixel 27 blue** — Blue of pixel 27.

**109 · Pixel 28 dimmer** — Dimmer of pixel 28. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**110 · Pixel 28 red** — Red of pixel 28.

**111 · Pixel 28 green** — Green of pixel 28.

**112 · Pixel 28 blue** — Blue of pixel 28.

**113 · Pixel 29 dimmer** — Dimmer of pixel 29. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**114 · Pixel 29 red** — Red of pixel 29.

**115 · Pixel 29 green** — Green of pixel 29.

**116 · Pixel 29 blue** — Blue of pixel 29.

**117 · Pixel 30 dimmer** — Dimmer of pixel 30. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**118 · Pixel 30 red** — Red of pixel 30.

**119 · Pixel 30 green** — Green of pixel 30.

**120 · Pixel 30 blue** — Blue of pixel 30.

**121 · Pixel 31 dimmer** — Dimmer of pixel 31. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**122 · Pixel 31 red** — Red of pixel 31.

**123 · Pixel 31 green** — Green of pixel 31.

**124 · Pixel 31 blue** — Blue of pixel 31.

**125 · Pixel 32 dimmer** — Dimmer of pixel 32. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**126 · Pixel 32 red** — Red of pixel 32.

**127 · Pixel 32 green** — Green of pixel 32.

**128 · Pixel 32 blue** — Blue of pixel 32.

**129 · Pixel 33 dimmer** — Dimmer of pixel 33. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**130 · Pixel 33 red** — Red of pixel 33.

**131 · Pixel 33 green** — Green of pixel 33.

**132 · Pixel 33 blue** — Blue of pixel 33.

**133 · Pixel 34 dimmer** — Dimmer of pixel 34. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**134 · Pixel 34 red** — Red of pixel 34.

**135 · Pixel 34 green** — Green of pixel 34.

**136 · Pixel 34 blue** — Blue of pixel 34.

**137 · Pixel 35 dimmer** — Dimmer of pixel 35. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**138 · Pixel 35 red** — Red of pixel 35.

**139 · Pixel 35 green** — Green of pixel 35.

**140 · Pixel 35 blue** — Blue of pixel 35.

**141 · Pixel 36 dimmer** — Dimmer of pixel 36. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**142 · Pixel 36 red** — Red of pixel 36.

**143 · Pixel 36 green** — Green of pixel 36.

**144 · Pixel 36 blue** — Blue of pixel 36.

**145 · Pixel 37 dimmer** — Dimmer of pixel 37. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**146 · Pixel 37 red** — Red of pixel 37.

**147 · Pixel 37 green** — Green of pixel 37.

**148 · Pixel 37 blue** — Blue of pixel 37.

**149 · Pixel 38 dimmer** — Dimmer of pixel 38. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**150 · Pixel 38 red** — Red of pixel 38.

**151 · Pixel 38 green** — Green of pixel 38.

**152 · Pixel 38 blue** — Blue of pixel 38.

**153 · Pixel 39 dimmer** — Dimmer of pixel 39. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**154 · Pixel 39 red** — Red of pixel 39.

**155 · Pixel 39 green** — Green of pixel 39.

**156 · Pixel 39 blue** — Blue of pixel 39.

**157 · Pixel 40 dimmer** — Dimmer of pixel 40. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**158 · Pixel 40 red** — Red of pixel 40.

**159 · Pixel 40 green** — Green of pixel 40.

**160 · Pixel 40 blue** — Blue of pixel 40.

**161 · Pixel 41 dimmer** — Dimmer of pixel 41. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**162 · Pixel 41 red** — Red of pixel 41.

**163 · Pixel 41 green** — Green of pixel 41.

**164 · Pixel 41 blue** — Blue of pixel 41.

**165 · Pixel 42 dimmer** — Dimmer of pixel 42. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**166 · Pixel 42 red** — Red of pixel 42.

**167 · Pixel 42 green** — Green of pixel 42.

**168 · Pixel 42 blue** — Blue of pixel 42.

**169 · Pixel 43 dimmer** — Dimmer of pixel 43. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**170 · Pixel 43 red** — Red of pixel 43.

**171 · Pixel 43 green** — Green of pixel 43.

**172 · Pixel 43 blue** — Blue of pixel 43.

**173 · Pixel 44 dimmer** — Dimmer of pixel 44. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**174 · Pixel 44 red** — Red of pixel 44.

**175 · Pixel 44 green** — Green of pixel 44.

**176 · Pixel 44 blue** — Blue of pixel 44.

**177 · Pixel 45 dimmer** — Dimmer of pixel 45. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**178 · Pixel 45 red** — Red of pixel 45.

**179 · Pixel 45 green** — Green of pixel 45.

**180 · Pixel 45 blue** — Blue of pixel 45.

**181 · Pixel 46 dimmer** — Dimmer of pixel 46. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**182 · Pixel 46 red** — Red of pixel 46.

**183 · Pixel 46 green** — Green of pixel 46.

**184 · Pixel 46 blue** — Blue of pixel 46.
