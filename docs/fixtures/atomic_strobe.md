# Atomic Strobe

`theatricalextralights:atomic_strobe` · family: [Blinders & strobes](/fixtures/blinders-strobes)

LED strobe in the Atomic style: a nine-segment white strobe bar between two plates of four RGB zones, on a U yoke with a floor plate.

- Every flash is timed from the game clock, so all clients see the same flashes. A flash shorter than a frame still shows for one frame.
- The room light follows the flashes tick by tick.

## Personalities

| Mode | Channels |
|---|---|
| 34-Channel Pixel (Legacy) | 34 |
| 1-Channel Strobe | 1 |
| 4-Channel Atomic | 4 |
| 8-Channel Atomic + Aura | 8 |
| 10-Channel Compressed (Bar + Plate) | 10 |
| 40-Channel Pixel (Bar + Plate) | 40 |
| 400-Channel Pixel Map (Bar + 96 Pixels) | 400 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 34-Channel Pixel (Legacy) (34 ch)

The original layout, everything lit continuously at its level. Kept first so fixtures placed before the modes existed keep working.

| Ch | Function | Values |
|---|---|---|
| 1 | Zone 1 red | 0 to 255 |
| 2 | Zone 1 green | 0 to 255 |
| 3 | Zone 1 blue | 0 to 255 |
| 4 | Zone 2 red | 0 to 255 |
| 5 | Zone 2 green | 0 to 255 |
| 6 | Zone 2 blue | 0 to 255 |
| 7 | Zone 3 red | 0 to 255 |
| 8 | Zone 3 green | 0 to 255 |
| 9 | Zone 3 blue | 0 to 255 |
| 10 | Zone 4 red | 0 to 255 |
| 11 | Zone 4 green | 0 to 255 |
| 12 | Zone 4 blue | 0 to 255 |
| 13 | Zone 5 red | 0 to 255 |
| 14 | Zone 5 green | 0 to 255 |
| 15 | Zone 5 blue | 0 to 255 |
| 16 | Zone 6 red | 0 to 255 |
| 17 | Zone 6 green | 0 to 255 |
| 18 | Zone 6 blue | 0 to 255 |
| 19 | Zone 7 red | 0 to 255 |
| 20 | Zone 7 green | 0 to 255 |
| 21 | Zone 7 blue | 0 to 255 |
| 22 | Zone 8 red | 0 to 255 |
| 23 | Zone 8 green | 0 to 255 |
| 24 | Zone 8 blue | 0 to 255 |
| 25 | White bar segment 1 | 0 to 255 |
| 26 | White bar segment 2 | 0 to 255 |
| 27 | White bar segment 3 | 0 to 255 |
| 28 | White bar segment 4 | 0 to 255 |
| 29 | White bar segment 5 | 0 to 255 |
| 30 | White bar segment 6 | 0 to 255 |
| 31 | White bar segment 7 | 0 to 255 |
| 32 | White bar segment 8 | 0 to 255 |
| 33 | White bar segment 9 | 0 to 255 |
| 34 | Focus | 1 to 255 |

### Channel by channel

**1 · Zone 1 red** — Red of RGB zone 1.

**2 · Zone 1 green** — Green of RGB zone 1.

**3 · Zone 1 blue** — Blue of RGB zone 1.

**4 · Zone 2 red** — Red of RGB zone 2.

**5 · Zone 2 green** — Green of RGB zone 2.

**6 · Zone 2 blue** — Blue of RGB zone 2.

**7 · Zone 3 red** — Red of RGB zone 3.

**8 · Zone 3 green** — Green of RGB zone 3.

**9 · Zone 3 blue** — Blue of RGB zone 3.

**10 · Zone 4 red** — Red of RGB zone 4.

**11 · Zone 4 green** — Green of RGB zone 4.

**12 · Zone 4 blue** — Blue of RGB zone 4.

**13 · Zone 5 red** — Red of RGB zone 5.

**14 · Zone 5 green** — Green of RGB zone 5.

**15 · Zone 5 blue** — Blue of RGB zone 5.

**16 · Zone 6 red** — Red of RGB zone 6.

**17 · Zone 6 green** — Green of RGB zone 6.

**18 · Zone 6 blue** — Blue of RGB zone 6.

**19 · Zone 7 red** — Red of RGB zone 7.

**20 · Zone 7 green** — Green of RGB zone 7.

**21 · Zone 7 blue** — Blue of RGB zone 7.

**22 · Zone 8 red** — Red of RGB zone 8.

**23 · Zone 8 green** — Green of RGB zone 8.

**24 · Zone 8 blue** — Blue of RGB zone 8.

**25 · White bar segment 1** — Level of white bar segment 1.

**26 · White bar segment 2** — Level of white bar segment 2.

**27 · White bar segment 3** — Level of white bar segment 3.

**28 · White bar segment 4** — Level of white bar segment 4.

**29 · White bar segment 5** — Level of white bar segment 5.

**30 · White bar segment 6** — Level of white bar segment 6.

**31 · White bar segment 7** — Level of white bar segment 7.

**32 · White bar segment 8** — Level of white bar segment 8.

**33 · White bar segment 9** — Level of white bar segment 9.

**34 · Focus** — Size of the light spot on the ground.

## 1-Channel Strobe (1 ch)

White bar only.

| Ch | Function | Values |
|---|---|---|
| 1 | Strobe | 0 to 255 |

### Channel by channel

**1 · Strobe** — 0 off; 1 to 254 flash rate, slow to fast, with a short flash; 255 lamp on continuously.

## 4-Channel Atomic (4 ch)

White bar only, driven like a Martin Atomic 3000.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 to 255 |
| 2 | Flash duration | 0 to 255 |
| 3 | Flash rate | 0 to 255 |
| 4 | Effects | 0 to 255 |

### Channel by channel

**1 · Intensity** — Flash intensity.

**2 · Flash duration** — Length of each flash, 12 ms at 0 to 650 ms at 255.

**3 · Flash rate** — 0 stops the strobe (as on the real unit); 1 to 255 runs 0.5 to 25 flashes a second.

**4 · Effects** — 0-9 plain strobe; 10-39 blinder, lamp on continuously; 40-69 ramp up; 70-99 ramp down; 100-129 ramp up and down; 130-159 random; 160-189 lightning bursts; 190-219 spikes; 220-255 sparkle (each bar segment flashes on its own in the pixel modes).

## 8-Channel Atomic + Aura (8 ch)

The Atomic 3000 LED layout: the strobe bar plus the RGB plates as one coloured glow around it.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 to 255 |
| 2 | Flash duration | 0 to 255 |
| 3 | Flash rate | 0 to 255 |
| 4 | Effects | 0 to 255 |
| 5 | Aura intensity | 0 to 255 |
| 6 | Aura red | 0 to 255 |
| 7 | Aura green | 0 to 255 |
| 8 | Aura blue | 0 to 255 |

### Channel by channel

**1 · Intensity** — Flash intensity.

**2 · Flash duration** — Length of each flash, 12 ms at 0 to 650 ms at 255.

**3 · Flash rate** — 0 stops the strobe (as on the real unit); 1 to 255 runs 0.5 to 25 flashes a second.

**4 · Effects** — 0-9 plain strobe; 10-39 blinder, lamp on continuously; 40-69 ramp up; 70-99 ramp down; 100-129 ramp up and down; 130-159 random; 160-189 lightning bursts; 190-219 spikes; 220-255 sparkle (each bar segment flashes on its own in the pixel modes).

**5 · Aura intensity** — Level of the RGB plates, lit continuously as a backlight.

**6 · Aura red** — Red of the aura.

**7 · Aura green** — Green of the aura.

**8 · Aura blue** — Blue of the aura.

## 10-Channel Compressed (Bar + Plate) (10 ch)

All eight RGB zones act as one pixel with its own strobe, next to the bar.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 to 255 |
| 2 | Flash duration | 0 to 255 |
| 3 | Flash rate | 0 to 255 |
| 4 | Effects | 0 to 255 |
| 5 | Plate intensity | 0 to 255 |
| 6 | Plate flash duration | 0 to 255 |
| 7 | Plate flash rate | 0 to 255 |
| 8 | Plate red | 0 to 255 |
| 9 | Plate green | 0 to 255 |
| 10 | Plate blue | 0 to 255 |

### Channel by channel

**1 · Intensity** — Flash intensity.

**2 · Flash duration** — Length of each flash, 12 ms at 0 to 650 ms at 255.

**3 · Flash rate** — 0 stops the strobe (as on the real unit); 1 to 255 runs 0.5 to 25 flashes a second.

**4 · Effects** — 0-9 plain strobe; 10-39 blinder, lamp on continuously; 40-69 ramp up; 70-99 ramp down; 100-129 ramp up and down; 130-159 random; 160-189 lightning bursts; 190-219 spikes; 220-255 sparkle (each bar segment flashes on its own in the pixel modes).

**5 · Plate intensity** — Level of the RGB plates.

**6 · Plate flash duration** — Flash length of the plates, 12 ms to 650 ms.

**7 · Plate flash rate** — 0 leaves the plates on continuously; 1 to 255 strobes them at 0.5 to 25 Hz.

**8 · Plate red** — Red of all eight zones.

**9 · Plate green** — Green of all eight zones.

**10 · Plate blue** — Blue of all eight zones.

## 40-Channel Pixel (Bar + Plate) (40 ch)

Every segment and every zone on its own channel; the sparkle effect flashes segments individually.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 to 255 |
| 2 | Flash duration | 0 to 255 |
| 3 | Flash rate | 0 to 255 |
| 4 | Effects | 0 to 255 |
| 5 | Bar segment 1 | 0 to 255 |
| 6 | Bar segment 2 | 0 to 255 |
| 7 | Bar segment 3 | 0 to 255 |
| 8 | Bar segment 4 | 0 to 255 |
| 9 | Bar segment 5 | 0 to 255 |
| 10 | Bar segment 6 | 0 to 255 |
| 11 | Bar segment 7 | 0 to 255 |
| 12 | Bar segment 8 | 0 to 255 |
| 13 | Bar segment 9 | 0 to 255 |
| 14 | Plate intensity | 0 to 255 |
| 15 | Plate flash duration | 0 to 255 |
| 16 | Plate flash rate | 0 to 255 |
| 17 | Zone 1 red | 0 to 255 |
| 18 | Zone 1 green | 0 to 255 |
| 19 | Zone 1 blue | 0 to 255 |
| 20 | Zone 2 red | 0 to 255 |
| 21 | Zone 2 green | 0 to 255 |
| 22 | Zone 2 blue | 0 to 255 |
| 23 | Zone 3 red | 0 to 255 |
| 24 | Zone 3 green | 0 to 255 |
| 25 | Zone 3 blue | 0 to 255 |
| 26 | Zone 4 red | 0 to 255 |
| 27 | Zone 4 green | 0 to 255 |
| 28 | Zone 4 blue | 0 to 255 |
| 29 | Zone 5 red | 0 to 255 |
| 30 | Zone 5 green | 0 to 255 |
| 31 | Zone 5 blue | 0 to 255 |
| 32 | Zone 6 red | 0 to 255 |
| 33 | Zone 6 green | 0 to 255 |
| 34 | Zone 6 blue | 0 to 255 |
| 35 | Zone 7 red | 0 to 255 |
| 36 | Zone 7 green | 0 to 255 |
| 37 | Zone 7 blue | 0 to 255 |
| 38 | Zone 8 red | 0 to 255 |
| 39 | Zone 8 green | 0 to 255 |
| 40 | Zone 8 blue | 0 to 255 |

### Channel by channel

**1 · Intensity** — Flash intensity.

**2 · Flash duration** — Length of each flash, 12 ms at 0 to 650 ms at 255.

**3 · Flash rate** — 0 stops the strobe (as on the real unit); 1 to 255 runs 0.5 to 25 flashes a second.

**4 · Effects** — 0-9 plain strobe; 10-39 blinder, lamp on continuously; 40-69 ramp up; 70-99 ramp down; 100-129 ramp up and down; 130-159 random; 160-189 lightning bursts; 190-219 spikes; 220-255 sparkle (each bar segment flashes on its own in the pixel modes).

**5 · Bar segment 1** — Level of white bar segment 1 under the bar's flashes.

**6 · Bar segment 2** — Level of white bar segment 2 under the bar's flashes.

**7 · Bar segment 3** — Level of white bar segment 3 under the bar's flashes.

**8 · Bar segment 4** — Level of white bar segment 4 under the bar's flashes.

**9 · Bar segment 5** — Level of white bar segment 5 under the bar's flashes.

**10 · Bar segment 6** — Level of white bar segment 6 under the bar's flashes.

**11 · Bar segment 7** — Level of white bar segment 7 under the bar's flashes.

**12 · Bar segment 8** — Level of white bar segment 8 under the bar's flashes.

**13 · Bar segment 9** — Level of white bar segment 9 under the bar's flashes.

**14 · Plate intensity** — Level of the RGB plates.

**15 · Plate flash duration** — Flash length of the plates.

**16 · Plate flash rate** — 0 continuous; 1 to 255 strobes the plates.

**17 · Zone 1 red** — Red of RGB zone 1.

**18 · Zone 1 green** — Green of RGB zone 1.

**19 · Zone 1 blue** — Blue of RGB zone 1.

**20 · Zone 2 red** — Red of RGB zone 2.

**21 · Zone 2 green** — Green of RGB zone 2.

**22 · Zone 2 blue** — Blue of RGB zone 2.

**23 · Zone 3 red** — Red of RGB zone 3.

**24 · Zone 3 green** — Green of RGB zone 3.

**25 · Zone 3 blue** — Blue of RGB zone 3.

**26 · Zone 4 red** — Red of RGB zone 4.

**27 · Zone 4 green** — Green of RGB zone 4.

**28 · Zone 4 blue** — Blue of RGB zone 4.

**29 · Zone 5 red** — Red of RGB zone 5.

**30 · Zone 5 green** — Green of RGB zone 5.

**31 · Zone 5 blue** — Blue of RGB zone 5.

**32 · Zone 6 red** — Red of RGB zone 6.

**33 · Zone 6 green** — Green of RGB zone 6.

**34 · Zone 6 blue** — Blue of RGB zone 6.

**35 · Zone 7 red** — Red of RGB zone 7.

**36 · Zone 7 green** — Green of RGB zone 7.

**37 · Zone 7 blue** — Blue of RGB zone 7.

**38 · Zone 8 red** — Red of RGB zone 8.

**39 · Zone 8 green** — Green of RGB zone 8.

**40 · Zone 8 blue** — Blue of RGB zone 8.

## 400-Channel Pixel Map (Bar + 96 Pixels) (400 ch)

Each plate is a grid of 12 columns by 4 rows. Pixels 1 to 48 are the top plate, 49 to 96 the bottom plate, row by row from the top, left to right, four channels each. Fits one universe.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 to 255 |
| 2 | Flash duration | 0 to 255 |
| 3 | Flash rate | 0 to 255 |
| 4 | Effects | 0 to 255 |
| 5 | Bar segment 1 | 0 to 255 |
| 6 | Bar segment 2 | 0 to 255 |
| 7 | Bar segment 3 | 0 to 255 |
| 8 | Bar segment 4 | 0 to 255 |
| 9 | Bar segment 5 | 0 to 255 |
| 10 | Bar segment 6 | 0 to 255 |
| 11 | Bar segment 7 | 0 to 255 |
| 12 | Bar segment 8 | 0 to 255 |
| 13 | Bar segment 9 | 0 to 255 |
| 14 | Plate intensity | 0 to 255 |
| 15 | Plate flash duration | 0 to 255 |
| 16 | Plate flash rate | 0 to 255 |
| 17 | Pixel 1 dim | 0 to 255 |
| 18 | Pixel 1 red | 0 to 255 |
| 19 | Pixel 1 green | 0 to 255 |
| 20 | Pixel 1 blue | 0 to 255 |
| 21 | Pixel 2 dim | 0 to 255 |
| 22 | Pixel 2 red | 0 to 255 |
| 23 | Pixel 2 green | 0 to 255 |
| 24 | Pixel 2 blue | 0 to 255 |
| 25 | Pixel 3 dim | 0 to 255 |
| 26 | Pixel 3 red | 0 to 255 |
| 27 | Pixel 3 green | 0 to 255 |
| 28 | Pixel 3 blue | 0 to 255 |
| 29 | Pixel 4 dim | 0 to 255 |
| 30 | Pixel 4 red | 0 to 255 |
| 31 | Pixel 4 green | 0 to 255 |
| 32 | Pixel 4 blue | 0 to 255 |
| 33 | Pixel 5 dim | 0 to 255 |
| 34 | Pixel 5 red | 0 to 255 |
| 35 | Pixel 5 green | 0 to 255 |
| 36 | Pixel 5 blue | 0 to 255 |
| 37 | Pixel 6 dim | 0 to 255 |
| 38 | Pixel 6 red | 0 to 255 |
| 39 | Pixel 6 green | 0 to 255 |
| 40 | Pixel 6 blue | 0 to 255 |
| 41 | Pixel 7 dim | 0 to 255 |
| 42 | Pixel 7 red | 0 to 255 |
| 43 | Pixel 7 green | 0 to 255 |
| 44 | Pixel 7 blue | 0 to 255 |
| 45 | Pixel 8 dim | 0 to 255 |
| 46 | Pixel 8 red | 0 to 255 |
| 47 | Pixel 8 green | 0 to 255 |
| 48 | Pixel 8 blue | 0 to 255 |
| 49 | Pixel 9 dim | 0 to 255 |
| 50 | Pixel 9 red | 0 to 255 |
| 51 | Pixel 9 green | 0 to 255 |
| 52 | Pixel 9 blue | 0 to 255 |
| 53 | Pixel 10 dim | 0 to 255 |
| 54 | Pixel 10 red | 0 to 255 |
| 55 | Pixel 10 green | 0 to 255 |
| 56 | Pixel 10 blue | 0 to 255 |
| 57 | Pixel 11 dim | 0 to 255 |
| 58 | Pixel 11 red | 0 to 255 |
| 59 | Pixel 11 green | 0 to 255 |
| 60 | Pixel 11 blue | 0 to 255 |
| 61 | Pixel 12 dim | 0 to 255 |
| 62 | Pixel 12 red | 0 to 255 |
| 63 | Pixel 12 green | 0 to 255 |
| 64 | Pixel 12 blue | 0 to 255 |
| 65 | Pixel 13 dim | 0 to 255 |
| 66 | Pixel 13 red | 0 to 255 |
| 67 | Pixel 13 green | 0 to 255 |
| 68 | Pixel 13 blue | 0 to 255 |
| 69 | Pixel 14 dim | 0 to 255 |
| 70 | Pixel 14 red | 0 to 255 |
| 71 | Pixel 14 green | 0 to 255 |
| 72 | Pixel 14 blue | 0 to 255 |
| 73 | Pixel 15 dim | 0 to 255 |
| 74 | Pixel 15 red | 0 to 255 |
| 75 | Pixel 15 green | 0 to 255 |
| 76 | Pixel 15 blue | 0 to 255 |
| 77 | Pixel 16 dim | 0 to 255 |
| 78 | Pixel 16 red | 0 to 255 |
| 79 | Pixel 16 green | 0 to 255 |
| 80 | Pixel 16 blue | 0 to 255 |
| 81 | Pixel 17 dim | 0 to 255 |
| 82 | Pixel 17 red | 0 to 255 |
| 83 | Pixel 17 green | 0 to 255 |
| 84 | Pixel 17 blue | 0 to 255 |
| 85 | Pixel 18 dim | 0 to 255 |
| 86 | Pixel 18 red | 0 to 255 |
| 87 | Pixel 18 green | 0 to 255 |
| 88 | Pixel 18 blue | 0 to 255 |
| 89 | Pixel 19 dim | 0 to 255 |
| 90 | Pixel 19 red | 0 to 255 |
| 91 | Pixel 19 green | 0 to 255 |
| 92 | Pixel 19 blue | 0 to 255 |
| 93 | Pixel 20 dim | 0 to 255 |
| 94 | Pixel 20 red | 0 to 255 |
| 95 | Pixel 20 green | 0 to 255 |
| 96 | Pixel 20 blue | 0 to 255 |
| 97 | Pixel 21 dim | 0 to 255 |
| 98 | Pixel 21 red | 0 to 255 |
| 99 | Pixel 21 green | 0 to 255 |
| 100 | Pixel 21 blue | 0 to 255 |
| 101 | Pixel 22 dim | 0 to 255 |
| 102 | Pixel 22 red | 0 to 255 |
| 103 | Pixel 22 green | 0 to 255 |
| 104 | Pixel 22 blue | 0 to 255 |
| 105 | Pixel 23 dim | 0 to 255 |
| 106 | Pixel 23 red | 0 to 255 |
| 107 | Pixel 23 green | 0 to 255 |
| 108 | Pixel 23 blue | 0 to 255 |
| 109 | Pixel 24 dim | 0 to 255 |
| 110 | Pixel 24 red | 0 to 255 |
| 111 | Pixel 24 green | 0 to 255 |
| 112 | Pixel 24 blue | 0 to 255 |
| 113 | Pixel 25 dim | 0 to 255 |
| 114 | Pixel 25 red | 0 to 255 |
| 115 | Pixel 25 green | 0 to 255 |
| 116 | Pixel 25 blue | 0 to 255 |
| 117 | Pixel 26 dim | 0 to 255 |
| 118 | Pixel 26 red | 0 to 255 |
| 119 | Pixel 26 green | 0 to 255 |
| 120 | Pixel 26 blue | 0 to 255 |
| 121 | Pixel 27 dim | 0 to 255 |
| 122 | Pixel 27 red | 0 to 255 |
| 123 | Pixel 27 green | 0 to 255 |
| 124 | Pixel 27 blue | 0 to 255 |
| 125 | Pixel 28 dim | 0 to 255 |
| 126 | Pixel 28 red | 0 to 255 |
| 127 | Pixel 28 green | 0 to 255 |
| 128 | Pixel 28 blue | 0 to 255 |
| 129 | Pixel 29 dim | 0 to 255 |
| 130 | Pixel 29 red | 0 to 255 |
| 131 | Pixel 29 green | 0 to 255 |
| 132 | Pixel 29 blue | 0 to 255 |
| 133 | Pixel 30 dim | 0 to 255 |
| 134 | Pixel 30 red | 0 to 255 |
| 135 | Pixel 30 green | 0 to 255 |
| 136 | Pixel 30 blue | 0 to 255 |
| 137 | Pixel 31 dim | 0 to 255 |
| 138 | Pixel 31 red | 0 to 255 |
| 139 | Pixel 31 green | 0 to 255 |
| 140 | Pixel 31 blue | 0 to 255 |
| 141 | Pixel 32 dim | 0 to 255 |
| 142 | Pixel 32 red | 0 to 255 |
| 143 | Pixel 32 green | 0 to 255 |
| 144 | Pixel 32 blue | 0 to 255 |
| 145 | Pixel 33 dim | 0 to 255 |
| 146 | Pixel 33 red | 0 to 255 |
| 147 | Pixel 33 green | 0 to 255 |
| 148 | Pixel 33 blue | 0 to 255 |
| 149 | Pixel 34 dim | 0 to 255 |
| 150 | Pixel 34 red | 0 to 255 |
| 151 | Pixel 34 green | 0 to 255 |
| 152 | Pixel 34 blue | 0 to 255 |
| 153 | Pixel 35 dim | 0 to 255 |
| 154 | Pixel 35 red | 0 to 255 |
| 155 | Pixel 35 green | 0 to 255 |
| 156 | Pixel 35 blue | 0 to 255 |
| 157 | Pixel 36 dim | 0 to 255 |
| 158 | Pixel 36 red | 0 to 255 |
| 159 | Pixel 36 green | 0 to 255 |
| 160 | Pixel 36 blue | 0 to 255 |
| 161 | Pixel 37 dim | 0 to 255 |
| 162 | Pixel 37 red | 0 to 255 |
| 163 | Pixel 37 green | 0 to 255 |
| 164 | Pixel 37 blue | 0 to 255 |
| 165 | Pixel 38 dim | 0 to 255 |
| 166 | Pixel 38 red | 0 to 255 |
| 167 | Pixel 38 green | 0 to 255 |
| 168 | Pixel 38 blue | 0 to 255 |
| 169 | Pixel 39 dim | 0 to 255 |
| 170 | Pixel 39 red | 0 to 255 |
| 171 | Pixel 39 green | 0 to 255 |
| 172 | Pixel 39 blue | 0 to 255 |
| 173 | Pixel 40 dim | 0 to 255 |
| 174 | Pixel 40 red | 0 to 255 |
| 175 | Pixel 40 green | 0 to 255 |
| 176 | Pixel 40 blue | 0 to 255 |
| 177 | Pixel 41 dim | 0 to 255 |
| 178 | Pixel 41 red | 0 to 255 |
| 179 | Pixel 41 green | 0 to 255 |
| 180 | Pixel 41 blue | 0 to 255 |
| 181 | Pixel 42 dim | 0 to 255 |
| 182 | Pixel 42 red | 0 to 255 |
| 183 | Pixel 42 green | 0 to 255 |
| 184 | Pixel 42 blue | 0 to 255 |
| 185 | Pixel 43 dim | 0 to 255 |
| 186 | Pixel 43 red | 0 to 255 |
| 187 | Pixel 43 green | 0 to 255 |
| 188 | Pixel 43 blue | 0 to 255 |
| 189 | Pixel 44 dim | 0 to 255 |
| 190 | Pixel 44 red | 0 to 255 |
| 191 | Pixel 44 green | 0 to 255 |
| 192 | Pixel 44 blue | 0 to 255 |
| 193 | Pixel 45 dim | 0 to 255 |
| 194 | Pixel 45 red | 0 to 255 |
| 195 | Pixel 45 green | 0 to 255 |
| 196 | Pixel 45 blue | 0 to 255 |
| 197 | Pixel 46 dim | 0 to 255 |
| 198 | Pixel 46 red | 0 to 255 |
| 199 | Pixel 46 green | 0 to 255 |
| 200 | Pixel 46 blue | 0 to 255 |
| 201 | Pixel 47 dim | 0 to 255 |
| 202 | Pixel 47 red | 0 to 255 |
| 203 | Pixel 47 green | 0 to 255 |
| 204 | Pixel 47 blue | 0 to 255 |
| 205 | Pixel 48 dim | 0 to 255 |
| 206 | Pixel 48 red | 0 to 255 |
| 207 | Pixel 48 green | 0 to 255 |
| 208 | Pixel 48 blue | 0 to 255 |
| 209 | Pixel 49 dim | 0 to 255 |
| 210 | Pixel 49 red | 0 to 255 |
| 211 | Pixel 49 green | 0 to 255 |
| 212 | Pixel 49 blue | 0 to 255 |
| 213 | Pixel 50 dim | 0 to 255 |
| 214 | Pixel 50 red | 0 to 255 |
| 215 | Pixel 50 green | 0 to 255 |
| 216 | Pixel 50 blue | 0 to 255 |
| 217 | Pixel 51 dim | 0 to 255 |
| 218 | Pixel 51 red | 0 to 255 |
| 219 | Pixel 51 green | 0 to 255 |
| 220 | Pixel 51 blue | 0 to 255 |
| 221 | Pixel 52 dim | 0 to 255 |
| 222 | Pixel 52 red | 0 to 255 |
| 223 | Pixel 52 green | 0 to 255 |
| 224 | Pixel 52 blue | 0 to 255 |
| 225 | Pixel 53 dim | 0 to 255 |
| 226 | Pixel 53 red | 0 to 255 |
| 227 | Pixel 53 green | 0 to 255 |
| 228 | Pixel 53 blue | 0 to 255 |
| 229 | Pixel 54 dim | 0 to 255 |
| 230 | Pixel 54 red | 0 to 255 |
| 231 | Pixel 54 green | 0 to 255 |
| 232 | Pixel 54 blue | 0 to 255 |
| 233 | Pixel 55 dim | 0 to 255 |
| 234 | Pixel 55 red | 0 to 255 |
| 235 | Pixel 55 green | 0 to 255 |
| 236 | Pixel 55 blue | 0 to 255 |
| 237 | Pixel 56 dim | 0 to 255 |
| 238 | Pixel 56 red | 0 to 255 |
| 239 | Pixel 56 green | 0 to 255 |
| 240 | Pixel 56 blue | 0 to 255 |
| 241 | Pixel 57 dim | 0 to 255 |
| 242 | Pixel 57 red | 0 to 255 |
| 243 | Pixel 57 green | 0 to 255 |
| 244 | Pixel 57 blue | 0 to 255 |
| 245 | Pixel 58 dim | 0 to 255 |
| 246 | Pixel 58 red | 0 to 255 |
| 247 | Pixel 58 green | 0 to 255 |
| 248 | Pixel 58 blue | 0 to 255 |
| 249 | Pixel 59 dim | 0 to 255 |
| 250 | Pixel 59 red | 0 to 255 |
| 251 | Pixel 59 green | 0 to 255 |
| 252 | Pixel 59 blue | 0 to 255 |
| 253 | Pixel 60 dim | 0 to 255 |
| 254 | Pixel 60 red | 0 to 255 |
| 255 | Pixel 60 green | 0 to 255 |
| 256 | Pixel 60 blue | 0 to 255 |
| 257 | Pixel 61 dim | 0 to 255 |
| 258 | Pixel 61 red | 0 to 255 |
| 259 | Pixel 61 green | 0 to 255 |
| 260 | Pixel 61 blue | 0 to 255 |
| 261 | Pixel 62 dim | 0 to 255 |
| 262 | Pixel 62 red | 0 to 255 |
| 263 | Pixel 62 green | 0 to 255 |
| 264 | Pixel 62 blue | 0 to 255 |
| 265 | Pixel 63 dim | 0 to 255 |
| 266 | Pixel 63 red | 0 to 255 |
| 267 | Pixel 63 green | 0 to 255 |
| 268 | Pixel 63 blue | 0 to 255 |
| 269 | Pixel 64 dim | 0 to 255 |
| 270 | Pixel 64 red | 0 to 255 |
| 271 | Pixel 64 green | 0 to 255 |
| 272 | Pixel 64 blue | 0 to 255 |
| 273 | Pixel 65 dim | 0 to 255 |
| 274 | Pixel 65 red | 0 to 255 |
| 275 | Pixel 65 green | 0 to 255 |
| 276 | Pixel 65 blue | 0 to 255 |
| 277 | Pixel 66 dim | 0 to 255 |
| 278 | Pixel 66 red | 0 to 255 |
| 279 | Pixel 66 green | 0 to 255 |
| 280 | Pixel 66 blue | 0 to 255 |
| 281 | Pixel 67 dim | 0 to 255 |
| 282 | Pixel 67 red | 0 to 255 |
| 283 | Pixel 67 green | 0 to 255 |
| 284 | Pixel 67 blue | 0 to 255 |
| 285 | Pixel 68 dim | 0 to 255 |
| 286 | Pixel 68 red | 0 to 255 |
| 287 | Pixel 68 green | 0 to 255 |
| 288 | Pixel 68 blue | 0 to 255 |
| 289 | Pixel 69 dim | 0 to 255 |
| 290 | Pixel 69 red | 0 to 255 |
| 291 | Pixel 69 green | 0 to 255 |
| 292 | Pixel 69 blue | 0 to 255 |
| 293 | Pixel 70 dim | 0 to 255 |
| 294 | Pixel 70 red | 0 to 255 |
| 295 | Pixel 70 green | 0 to 255 |
| 296 | Pixel 70 blue | 0 to 255 |
| 297 | Pixel 71 dim | 0 to 255 |
| 298 | Pixel 71 red | 0 to 255 |
| 299 | Pixel 71 green | 0 to 255 |
| 300 | Pixel 71 blue | 0 to 255 |
| 301 | Pixel 72 dim | 0 to 255 |
| 302 | Pixel 72 red | 0 to 255 |
| 303 | Pixel 72 green | 0 to 255 |
| 304 | Pixel 72 blue | 0 to 255 |
| 305 | Pixel 73 dim | 0 to 255 |
| 306 | Pixel 73 red | 0 to 255 |
| 307 | Pixel 73 green | 0 to 255 |
| 308 | Pixel 73 blue | 0 to 255 |
| 309 | Pixel 74 dim | 0 to 255 |
| 310 | Pixel 74 red | 0 to 255 |
| 311 | Pixel 74 green | 0 to 255 |
| 312 | Pixel 74 blue | 0 to 255 |
| 313 | Pixel 75 dim | 0 to 255 |
| 314 | Pixel 75 red | 0 to 255 |
| 315 | Pixel 75 green | 0 to 255 |
| 316 | Pixel 75 blue | 0 to 255 |
| 317 | Pixel 76 dim | 0 to 255 |
| 318 | Pixel 76 red | 0 to 255 |
| 319 | Pixel 76 green | 0 to 255 |
| 320 | Pixel 76 blue | 0 to 255 |
| 321 | Pixel 77 dim | 0 to 255 |
| 322 | Pixel 77 red | 0 to 255 |
| 323 | Pixel 77 green | 0 to 255 |
| 324 | Pixel 77 blue | 0 to 255 |
| 325 | Pixel 78 dim | 0 to 255 |
| 326 | Pixel 78 red | 0 to 255 |
| 327 | Pixel 78 green | 0 to 255 |
| 328 | Pixel 78 blue | 0 to 255 |
| 329 | Pixel 79 dim | 0 to 255 |
| 330 | Pixel 79 red | 0 to 255 |
| 331 | Pixel 79 green | 0 to 255 |
| 332 | Pixel 79 blue | 0 to 255 |
| 333 | Pixel 80 dim | 0 to 255 |
| 334 | Pixel 80 red | 0 to 255 |
| 335 | Pixel 80 green | 0 to 255 |
| 336 | Pixel 80 blue | 0 to 255 |
| 337 | Pixel 81 dim | 0 to 255 |
| 338 | Pixel 81 red | 0 to 255 |
| 339 | Pixel 81 green | 0 to 255 |
| 340 | Pixel 81 blue | 0 to 255 |
| 341 | Pixel 82 dim | 0 to 255 |
| 342 | Pixel 82 red | 0 to 255 |
| 343 | Pixel 82 green | 0 to 255 |
| 344 | Pixel 82 blue | 0 to 255 |
| 345 | Pixel 83 dim | 0 to 255 |
| 346 | Pixel 83 red | 0 to 255 |
| 347 | Pixel 83 green | 0 to 255 |
| 348 | Pixel 83 blue | 0 to 255 |
| 349 | Pixel 84 dim | 0 to 255 |
| 350 | Pixel 84 red | 0 to 255 |
| 351 | Pixel 84 green | 0 to 255 |
| 352 | Pixel 84 blue | 0 to 255 |
| 353 | Pixel 85 dim | 0 to 255 |
| 354 | Pixel 85 red | 0 to 255 |
| 355 | Pixel 85 green | 0 to 255 |
| 356 | Pixel 85 blue | 0 to 255 |
| 357 | Pixel 86 dim | 0 to 255 |
| 358 | Pixel 86 red | 0 to 255 |
| 359 | Pixel 86 green | 0 to 255 |
| 360 | Pixel 86 blue | 0 to 255 |
| 361 | Pixel 87 dim | 0 to 255 |
| 362 | Pixel 87 red | 0 to 255 |
| 363 | Pixel 87 green | 0 to 255 |
| 364 | Pixel 87 blue | 0 to 255 |
| 365 | Pixel 88 dim | 0 to 255 |
| 366 | Pixel 88 red | 0 to 255 |
| 367 | Pixel 88 green | 0 to 255 |
| 368 | Pixel 88 blue | 0 to 255 |
| 369 | Pixel 89 dim | 0 to 255 |
| 370 | Pixel 89 red | 0 to 255 |
| 371 | Pixel 89 green | 0 to 255 |
| 372 | Pixel 89 blue | 0 to 255 |
| 373 | Pixel 90 dim | 0 to 255 |
| 374 | Pixel 90 red | 0 to 255 |
| 375 | Pixel 90 green | 0 to 255 |
| 376 | Pixel 90 blue | 0 to 255 |
| 377 | Pixel 91 dim | 0 to 255 |
| 378 | Pixel 91 red | 0 to 255 |
| 379 | Pixel 91 green | 0 to 255 |
| 380 | Pixel 91 blue | 0 to 255 |
| 381 | Pixel 92 dim | 0 to 255 |
| 382 | Pixel 92 red | 0 to 255 |
| 383 | Pixel 92 green | 0 to 255 |
| 384 | Pixel 92 blue | 0 to 255 |
| 385 | Pixel 93 dim | 0 to 255 |
| 386 | Pixel 93 red | 0 to 255 |
| 387 | Pixel 93 green | 0 to 255 |
| 388 | Pixel 93 blue | 0 to 255 |
| 389 | Pixel 94 dim | 0 to 255 |
| 390 | Pixel 94 red | 0 to 255 |
| 391 | Pixel 94 green | 0 to 255 |
| 392 | Pixel 94 blue | 0 to 255 |
| 393 | Pixel 95 dim | 0 to 255 |
| 394 | Pixel 95 red | 0 to 255 |
| 395 | Pixel 95 green | 0 to 255 |
| 396 | Pixel 95 blue | 0 to 255 |
| 397 | Pixel 96 dim | 0 to 255 |
| 398 | Pixel 96 red | 0 to 255 |
| 399 | Pixel 96 green | 0 to 255 |
| 400 | Pixel 96 blue | 0 to 255 |

### Channel by channel

**1 · Intensity** — Flash intensity.

**2 · Flash duration** — Length of each flash, 12 ms at 0 to 650 ms at 255.

**3 · Flash rate** — 0 stops the strobe (as on the real unit); 1 to 255 runs 0.5 to 25 flashes a second.

**4 · Effects** — 0-9 plain strobe; 10-39 blinder, lamp on continuously; 40-69 ramp up; 70-99 ramp down; 100-129 ramp up and down; 130-159 random; 160-189 lightning bursts; 190-219 spikes; 220-255 sparkle (each bar segment flashes on its own in the pixel modes).

**5 · Bar segment 1** — Level of white bar segment 1 under the bar's flashes.

**6 · Bar segment 2** — Level of white bar segment 2 under the bar's flashes.

**7 · Bar segment 3** — Level of white bar segment 3 under the bar's flashes.

**8 · Bar segment 4** — Level of white bar segment 4 under the bar's flashes.

**9 · Bar segment 5** — Level of white bar segment 5 under the bar's flashes.

**10 · Bar segment 6** — Level of white bar segment 6 under the bar's flashes.

**11 · Bar segment 7** — Level of white bar segment 7 under the bar's flashes.

**12 · Bar segment 8** — Level of white bar segment 8 under the bar's flashes.

**13 · Bar segment 9** — Level of white bar segment 9 under the bar's flashes.

**14 · Plate intensity** — Level of the RGB plates.

**15 · Plate flash duration** — Flash length of the plates.

**16 · Plate flash rate** — 0 continuous; 1 to 255 strobes the plates.

**17 · Pixel 1 dim** — Dimmer of pixel 1.

**18 · Pixel 1 red** — Red of pixel 1.

**19 · Pixel 1 green** — Green of pixel 1.

**20 · Pixel 1 blue** — Blue of pixel 1.

**21 · Pixel 2 dim** — Dimmer of pixel 2.

**22 · Pixel 2 red** — Red of pixel 2.

**23 · Pixel 2 green** — Green of pixel 2.

**24 · Pixel 2 blue** — Blue of pixel 2.

**25 · Pixel 3 dim** — Dimmer of pixel 3.

**26 · Pixel 3 red** — Red of pixel 3.

**27 · Pixel 3 green** — Green of pixel 3.

**28 · Pixel 3 blue** — Blue of pixel 3.

**29 · Pixel 4 dim** — Dimmer of pixel 4.

**30 · Pixel 4 red** — Red of pixel 4.

**31 · Pixel 4 green** — Green of pixel 4.

**32 · Pixel 4 blue** — Blue of pixel 4.

**33 · Pixel 5 dim** — Dimmer of pixel 5.

**34 · Pixel 5 red** — Red of pixel 5.

**35 · Pixel 5 green** — Green of pixel 5.

**36 · Pixel 5 blue** — Blue of pixel 5.

**37 · Pixel 6 dim** — Dimmer of pixel 6.

**38 · Pixel 6 red** — Red of pixel 6.

**39 · Pixel 6 green** — Green of pixel 6.

**40 · Pixel 6 blue** — Blue of pixel 6.

**41 · Pixel 7 dim** — Dimmer of pixel 7.

**42 · Pixel 7 red** — Red of pixel 7.

**43 · Pixel 7 green** — Green of pixel 7.

**44 · Pixel 7 blue** — Blue of pixel 7.

**45 · Pixel 8 dim** — Dimmer of pixel 8.

**46 · Pixel 8 red** — Red of pixel 8.

**47 · Pixel 8 green** — Green of pixel 8.

**48 · Pixel 8 blue** — Blue of pixel 8.

**49 · Pixel 9 dim** — Dimmer of pixel 9.

**50 · Pixel 9 red** — Red of pixel 9.

**51 · Pixel 9 green** — Green of pixel 9.

**52 · Pixel 9 blue** — Blue of pixel 9.

**53 · Pixel 10 dim** — Dimmer of pixel 10.

**54 · Pixel 10 red** — Red of pixel 10.

**55 · Pixel 10 green** — Green of pixel 10.

**56 · Pixel 10 blue** — Blue of pixel 10.

**57 · Pixel 11 dim** — Dimmer of pixel 11.

**58 · Pixel 11 red** — Red of pixel 11.

**59 · Pixel 11 green** — Green of pixel 11.

**60 · Pixel 11 blue** — Blue of pixel 11.

**61 · Pixel 12 dim** — Dimmer of pixel 12.

**62 · Pixel 12 red** — Red of pixel 12.

**63 · Pixel 12 green** — Green of pixel 12.

**64 · Pixel 12 blue** — Blue of pixel 12.

**65 · Pixel 13 dim** — Dimmer of pixel 13.

**66 · Pixel 13 red** — Red of pixel 13.

**67 · Pixel 13 green** — Green of pixel 13.

**68 · Pixel 13 blue** — Blue of pixel 13.

**69 · Pixel 14 dim** — Dimmer of pixel 14.

**70 · Pixel 14 red** — Red of pixel 14.

**71 · Pixel 14 green** — Green of pixel 14.

**72 · Pixel 14 blue** — Blue of pixel 14.

**73 · Pixel 15 dim** — Dimmer of pixel 15.

**74 · Pixel 15 red** — Red of pixel 15.

**75 · Pixel 15 green** — Green of pixel 15.

**76 · Pixel 15 blue** — Blue of pixel 15.

**77 · Pixel 16 dim** — Dimmer of pixel 16.

**78 · Pixel 16 red** — Red of pixel 16.

**79 · Pixel 16 green** — Green of pixel 16.

**80 · Pixel 16 blue** — Blue of pixel 16.

**81 · Pixel 17 dim** — Dimmer of pixel 17.

**82 · Pixel 17 red** — Red of pixel 17.

**83 · Pixel 17 green** — Green of pixel 17.

**84 · Pixel 17 blue** — Blue of pixel 17.

**85 · Pixel 18 dim** — Dimmer of pixel 18.

**86 · Pixel 18 red** — Red of pixel 18.

**87 · Pixel 18 green** — Green of pixel 18.

**88 · Pixel 18 blue** — Blue of pixel 18.

**89 · Pixel 19 dim** — Dimmer of pixel 19.

**90 · Pixel 19 red** — Red of pixel 19.

**91 · Pixel 19 green** — Green of pixel 19.

**92 · Pixel 19 blue** — Blue of pixel 19.

**93 · Pixel 20 dim** — Dimmer of pixel 20.

**94 · Pixel 20 red** — Red of pixel 20.

**95 · Pixel 20 green** — Green of pixel 20.

**96 · Pixel 20 blue** — Blue of pixel 20.

**97 · Pixel 21 dim** — Dimmer of pixel 21.

**98 · Pixel 21 red** — Red of pixel 21.

**99 · Pixel 21 green** — Green of pixel 21.

**100 · Pixel 21 blue** — Blue of pixel 21.

**101 · Pixel 22 dim** — Dimmer of pixel 22.

**102 · Pixel 22 red** — Red of pixel 22.

**103 · Pixel 22 green** — Green of pixel 22.

**104 · Pixel 22 blue** — Blue of pixel 22.

**105 · Pixel 23 dim** — Dimmer of pixel 23.

**106 · Pixel 23 red** — Red of pixel 23.

**107 · Pixel 23 green** — Green of pixel 23.

**108 · Pixel 23 blue** — Blue of pixel 23.

**109 · Pixel 24 dim** — Dimmer of pixel 24.

**110 · Pixel 24 red** — Red of pixel 24.

**111 · Pixel 24 green** — Green of pixel 24.

**112 · Pixel 24 blue** — Blue of pixel 24.

**113 · Pixel 25 dim** — Dimmer of pixel 25.

**114 · Pixel 25 red** — Red of pixel 25.

**115 · Pixel 25 green** — Green of pixel 25.

**116 · Pixel 25 blue** — Blue of pixel 25.

**117 · Pixel 26 dim** — Dimmer of pixel 26.

**118 · Pixel 26 red** — Red of pixel 26.

**119 · Pixel 26 green** — Green of pixel 26.

**120 · Pixel 26 blue** — Blue of pixel 26.

**121 · Pixel 27 dim** — Dimmer of pixel 27.

**122 · Pixel 27 red** — Red of pixel 27.

**123 · Pixel 27 green** — Green of pixel 27.

**124 · Pixel 27 blue** — Blue of pixel 27.

**125 · Pixel 28 dim** — Dimmer of pixel 28.

**126 · Pixel 28 red** — Red of pixel 28.

**127 · Pixel 28 green** — Green of pixel 28.

**128 · Pixel 28 blue** — Blue of pixel 28.

**129 · Pixel 29 dim** — Dimmer of pixel 29.

**130 · Pixel 29 red** — Red of pixel 29.

**131 · Pixel 29 green** — Green of pixel 29.

**132 · Pixel 29 blue** — Blue of pixel 29.

**133 · Pixel 30 dim** — Dimmer of pixel 30.

**134 · Pixel 30 red** — Red of pixel 30.

**135 · Pixel 30 green** — Green of pixel 30.

**136 · Pixel 30 blue** — Blue of pixel 30.

**137 · Pixel 31 dim** — Dimmer of pixel 31.

**138 · Pixel 31 red** — Red of pixel 31.

**139 · Pixel 31 green** — Green of pixel 31.

**140 · Pixel 31 blue** — Blue of pixel 31.

**141 · Pixel 32 dim** — Dimmer of pixel 32.

**142 · Pixel 32 red** — Red of pixel 32.

**143 · Pixel 32 green** — Green of pixel 32.

**144 · Pixel 32 blue** — Blue of pixel 32.

**145 · Pixel 33 dim** — Dimmer of pixel 33.

**146 · Pixel 33 red** — Red of pixel 33.

**147 · Pixel 33 green** — Green of pixel 33.

**148 · Pixel 33 blue** — Blue of pixel 33.

**149 · Pixel 34 dim** — Dimmer of pixel 34.

**150 · Pixel 34 red** — Red of pixel 34.

**151 · Pixel 34 green** — Green of pixel 34.

**152 · Pixel 34 blue** — Blue of pixel 34.

**153 · Pixel 35 dim** — Dimmer of pixel 35.

**154 · Pixel 35 red** — Red of pixel 35.

**155 · Pixel 35 green** — Green of pixel 35.

**156 · Pixel 35 blue** — Blue of pixel 35.

**157 · Pixel 36 dim** — Dimmer of pixel 36.

**158 · Pixel 36 red** — Red of pixel 36.

**159 · Pixel 36 green** — Green of pixel 36.

**160 · Pixel 36 blue** — Blue of pixel 36.

**161 · Pixel 37 dim** — Dimmer of pixel 37.

**162 · Pixel 37 red** — Red of pixel 37.

**163 · Pixel 37 green** — Green of pixel 37.

**164 · Pixel 37 blue** — Blue of pixel 37.

**165 · Pixel 38 dim** — Dimmer of pixel 38.

**166 · Pixel 38 red** — Red of pixel 38.

**167 · Pixel 38 green** — Green of pixel 38.

**168 · Pixel 38 blue** — Blue of pixel 38.

**169 · Pixel 39 dim** — Dimmer of pixel 39.

**170 · Pixel 39 red** — Red of pixel 39.

**171 · Pixel 39 green** — Green of pixel 39.

**172 · Pixel 39 blue** — Blue of pixel 39.

**173 · Pixel 40 dim** — Dimmer of pixel 40.

**174 · Pixel 40 red** — Red of pixel 40.

**175 · Pixel 40 green** — Green of pixel 40.

**176 · Pixel 40 blue** — Blue of pixel 40.

**177 · Pixel 41 dim** — Dimmer of pixel 41.

**178 · Pixel 41 red** — Red of pixel 41.

**179 · Pixel 41 green** — Green of pixel 41.

**180 · Pixel 41 blue** — Blue of pixel 41.

**181 · Pixel 42 dim** — Dimmer of pixel 42.

**182 · Pixel 42 red** — Red of pixel 42.

**183 · Pixel 42 green** — Green of pixel 42.

**184 · Pixel 42 blue** — Blue of pixel 42.

**185 · Pixel 43 dim** — Dimmer of pixel 43.

**186 · Pixel 43 red** — Red of pixel 43.

**187 · Pixel 43 green** — Green of pixel 43.

**188 · Pixel 43 blue** — Blue of pixel 43.

**189 · Pixel 44 dim** — Dimmer of pixel 44.

**190 · Pixel 44 red** — Red of pixel 44.

**191 · Pixel 44 green** — Green of pixel 44.

**192 · Pixel 44 blue** — Blue of pixel 44.

**193 · Pixel 45 dim** — Dimmer of pixel 45.

**194 · Pixel 45 red** — Red of pixel 45.

**195 · Pixel 45 green** — Green of pixel 45.

**196 · Pixel 45 blue** — Blue of pixel 45.

**197 · Pixel 46 dim** — Dimmer of pixel 46.

**198 · Pixel 46 red** — Red of pixel 46.

**199 · Pixel 46 green** — Green of pixel 46.

**200 · Pixel 46 blue** — Blue of pixel 46.

**201 · Pixel 47 dim** — Dimmer of pixel 47.

**202 · Pixel 47 red** — Red of pixel 47.

**203 · Pixel 47 green** — Green of pixel 47.

**204 · Pixel 47 blue** — Blue of pixel 47.

**205 · Pixel 48 dim** — Dimmer of pixel 48.

**206 · Pixel 48 red** — Red of pixel 48.

**207 · Pixel 48 green** — Green of pixel 48.

**208 · Pixel 48 blue** — Blue of pixel 48.

**209 · Pixel 49 dim** — Dimmer of pixel 49.

**210 · Pixel 49 red** — Red of pixel 49.

**211 · Pixel 49 green** — Green of pixel 49.

**212 · Pixel 49 blue** — Blue of pixel 49.

**213 · Pixel 50 dim** — Dimmer of pixel 50.

**214 · Pixel 50 red** — Red of pixel 50.

**215 · Pixel 50 green** — Green of pixel 50.

**216 · Pixel 50 blue** — Blue of pixel 50.

**217 · Pixel 51 dim** — Dimmer of pixel 51.

**218 · Pixel 51 red** — Red of pixel 51.

**219 · Pixel 51 green** — Green of pixel 51.

**220 · Pixel 51 blue** — Blue of pixel 51.

**221 · Pixel 52 dim** — Dimmer of pixel 52.

**222 · Pixel 52 red** — Red of pixel 52.

**223 · Pixel 52 green** — Green of pixel 52.

**224 · Pixel 52 blue** — Blue of pixel 52.

**225 · Pixel 53 dim** — Dimmer of pixel 53.

**226 · Pixel 53 red** — Red of pixel 53.

**227 · Pixel 53 green** — Green of pixel 53.

**228 · Pixel 53 blue** — Blue of pixel 53.

**229 · Pixel 54 dim** — Dimmer of pixel 54.

**230 · Pixel 54 red** — Red of pixel 54.

**231 · Pixel 54 green** — Green of pixel 54.

**232 · Pixel 54 blue** — Blue of pixel 54.

**233 · Pixel 55 dim** — Dimmer of pixel 55.

**234 · Pixel 55 red** — Red of pixel 55.

**235 · Pixel 55 green** — Green of pixel 55.

**236 · Pixel 55 blue** — Blue of pixel 55.

**237 · Pixel 56 dim** — Dimmer of pixel 56.

**238 · Pixel 56 red** — Red of pixel 56.

**239 · Pixel 56 green** — Green of pixel 56.

**240 · Pixel 56 blue** — Blue of pixel 56.

**241 · Pixel 57 dim** — Dimmer of pixel 57.

**242 · Pixel 57 red** — Red of pixel 57.

**243 · Pixel 57 green** — Green of pixel 57.

**244 · Pixel 57 blue** — Blue of pixel 57.

**245 · Pixel 58 dim** — Dimmer of pixel 58.

**246 · Pixel 58 red** — Red of pixel 58.

**247 · Pixel 58 green** — Green of pixel 58.

**248 · Pixel 58 blue** — Blue of pixel 58.

**249 · Pixel 59 dim** — Dimmer of pixel 59.

**250 · Pixel 59 red** — Red of pixel 59.

**251 · Pixel 59 green** — Green of pixel 59.

**252 · Pixel 59 blue** — Blue of pixel 59.

**253 · Pixel 60 dim** — Dimmer of pixel 60.

**254 · Pixel 60 red** — Red of pixel 60.

**255 · Pixel 60 green** — Green of pixel 60.

**256 · Pixel 60 blue** — Blue of pixel 60.

**257 · Pixel 61 dim** — Dimmer of pixel 61.

**258 · Pixel 61 red** — Red of pixel 61.

**259 · Pixel 61 green** — Green of pixel 61.

**260 · Pixel 61 blue** — Blue of pixel 61.

**261 · Pixel 62 dim** — Dimmer of pixel 62.

**262 · Pixel 62 red** — Red of pixel 62.

**263 · Pixel 62 green** — Green of pixel 62.

**264 · Pixel 62 blue** — Blue of pixel 62.

**265 · Pixel 63 dim** — Dimmer of pixel 63.

**266 · Pixel 63 red** — Red of pixel 63.

**267 · Pixel 63 green** — Green of pixel 63.

**268 · Pixel 63 blue** — Blue of pixel 63.

**269 · Pixel 64 dim** — Dimmer of pixel 64.

**270 · Pixel 64 red** — Red of pixel 64.

**271 · Pixel 64 green** — Green of pixel 64.

**272 · Pixel 64 blue** — Blue of pixel 64.

**273 · Pixel 65 dim** — Dimmer of pixel 65.

**274 · Pixel 65 red** — Red of pixel 65.

**275 · Pixel 65 green** — Green of pixel 65.

**276 · Pixel 65 blue** — Blue of pixel 65.

**277 · Pixel 66 dim** — Dimmer of pixel 66.

**278 · Pixel 66 red** — Red of pixel 66.

**279 · Pixel 66 green** — Green of pixel 66.

**280 · Pixel 66 blue** — Blue of pixel 66.

**281 · Pixel 67 dim** — Dimmer of pixel 67.

**282 · Pixel 67 red** — Red of pixel 67.

**283 · Pixel 67 green** — Green of pixel 67.

**284 · Pixel 67 blue** — Blue of pixel 67.

**285 · Pixel 68 dim** — Dimmer of pixel 68.

**286 · Pixel 68 red** — Red of pixel 68.

**287 · Pixel 68 green** — Green of pixel 68.

**288 · Pixel 68 blue** — Blue of pixel 68.

**289 · Pixel 69 dim** — Dimmer of pixel 69.

**290 · Pixel 69 red** — Red of pixel 69.

**291 · Pixel 69 green** — Green of pixel 69.

**292 · Pixel 69 blue** — Blue of pixel 69.

**293 · Pixel 70 dim** — Dimmer of pixel 70.

**294 · Pixel 70 red** — Red of pixel 70.

**295 · Pixel 70 green** — Green of pixel 70.

**296 · Pixel 70 blue** — Blue of pixel 70.

**297 · Pixel 71 dim** — Dimmer of pixel 71.

**298 · Pixel 71 red** — Red of pixel 71.

**299 · Pixel 71 green** — Green of pixel 71.

**300 · Pixel 71 blue** — Blue of pixel 71.

**301 · Pixel 72 dim** — Dimmer of pixel 72.

**302 · Pixel 72 red** — Red of pixel 72.

**303 · Pixel 72 green** — Green of pixel 72.

**304 · Pixel 72 blue** — Blue of pixel 72.

**305 · Pixel 73 dim** — Dimmer of pixel 73.

**306 · Pixel 73 red** — Red of pixel 73.

**307 · Pixel 73 green** — Green of pixel 73.

**308 · Pixel 73 blue** — Blue of pixel 73.

**309 · Pixel 74 dim** — Dimmer of pixel 74.

**310 · Pixel 74 red** — Red of pixel 74.

**311 · Pixel 74 green** — Green of pixel 74.

**312 · Pixel 74 blue** — Blue of pixel 74.

**313 · Pixel 75 dim** — Dimmer of pixel 75.

**314 · Pixel 75 red** — Red of pixel 75.

**315 · Pixel 75 green** — Green of pixel 75.

**316 · Pixel 75 blue** — Blue of pixel 75.

**317 · Pixel 76 dim** — Dimmer of pixel 76.

**318 · Pixel 76 red** — Red of pixel 76.

**319 · Pixel 76 green** — Green of pixel 76.

**320 · Pixel 76 blue** — Blue of pixel 76.

**321 · Pixel 77 dim** — Dimmer of pixel 77.

**322 · Pixel 77 red** — Red of pixel 77.

**323 · Pixel 77 green** — Green of pixel 77.

**324 · Pixel 77 blue** — Blue of pixel 77.

**325 · Pixel 78 dim** — Dimmer of pixel 78.

**326 · Pixel 78 red** — Red of pixel 78.

**327 · Pixel 78 green** — Green of pixel 78.

**328 · Pixel 78 blue** — Blue of pixel 78.

**329 · Pixel 79 dim** — Dimmer of pixel 79.

**330 · Pixel 79 red** — Red of pixel 79.

**331 · Pixel 79 green** — Green of pixel 79.

**332 · Pixel 79 blue** — Blue of pixel 79.

**333 · Pixel 80 dim** — Dimmer of pixel 80.

**334 · Pixel 80 red** — Red of pixel 80.

**335 · Pixel 80 green** — Green of pixel 80.

**336 · Pixel 80 blue** — Blue of pixel 80.

**337 · Pixel 81 dim** — Dimmer of pixel 81.

**338 · Pixel 81 red** — Red of pixel 81.

**339 · Pixel 81 green** — Green of pixel 81.

**340 · Pixel 81 blue** — Blue of pixel 81.

**341 · Pixel 82 dim** — Dimmer of pixel 82.

**342 · Pixel 82 red** — Red of pixel 82.

**343 · Pixel 82 green** — Green of pixel 82.

**344 · Pixel 82 blue** — Blue of pixel 82.

**345 · Pixel 83 dim** — Dimmer of pixel 83.

**346 · Pixel 83 red** — Red of pixel 83.

**347 · Pixel 83 green** — Green of pixel 83.

**348 · Pixel 83 blue** — Blue of pixel 83.

**349 · Pixel 84 dim** — Dimmer of pixel 84.

**350 · Pixel 84 red** — Red of pixel 84.

**351 · Pixel 84 green** — Green of pixel 84.

**352 · Pixel 84 blue** — Blue of pixel 84.

**353 · Pixel 85 dim** — Dimmer of pixel 85.

**354 · Pixel 85 red** — Red of pixel 85.

**355 · Pixel 85 green** — Green of pixel 85.

**356 · Pixel 85 blue** — Blue of pixel 85.

**357 · Pixel 86 dim** — Dimmer of pixel 86.

**358 · Pixel 86 red** — Red of pixel 86.

**359 · Pixel 86 green** — Green of pixel 86.

**360 · Pixel 86 blue** — Blue of pixel 86.

**361 · Pixel 87 dim** — Dimmer of pixel 87.

**362 · Pixel 87 red** — Red of pixel 87.

**363 · Pixel 87 green** — Green of pixel 87.

**364 · Pixel 87 blue** — Blue of pixel 87.

**365 · Pixel 88 dim** — Dimmer of pixel 88.

**366 · Pixel 88 red** — Red of pixel 88.

**367 · Pixel 88 green** — Green of pixel 88.

**368 · Pixel 88 blue** — Blue of pixel 88.

**369 · Pixel 89 dim** — Dimmer of pixel 89.

**370 · Pixel 89 red** — Red of pixel 89.

**371 · Pixel 89 green** — Green of pixel 89.

**372 · Pixel 89 blue** — Blue of pixel 89.

**373 · Pixel 90 dim** — Dimmer of pixel 90.

**374 · Pixel 90 red** — Red of pixel 90.

**375 · Pixel 90 green** — Green of pixel 90.

**376 · Pixel 90 blue** — Blue of pixel 90.

**377 · Pixel 91 dim** — Dimmer of pixel 91.

**378 · Pixel 91 red** — Red of pixel 91.

**379 · Pixel 91 green** — Green of pixel 91.

**380 · Pixel 91 blue** — Blue of pixel 91.

**381 · Pixel 92 dim** — Dimmer of pixel 92.

**382 · Pixel 92 red** — Red of pixel 92.

**383 · Pixel 92 green** — Green of pixel 92.

**384 · Pixel 92 blue** — Blue of pixel 92.

**385 · Pixel 93 dim** — Dimmer of pixel 93.

**386 · Pixel 93 red** — Red of pixel 93.

**387 · Pixel 93 green** — Green of pixel 93.

**388 · Pixel 93 blue** — Blue of pixel 93.

**389 · Pixel 94 dim** — Dimmer of pixel 94.

**390 · Pixel 94 red** — Red of pixel 94.

**391 · Pixel 94 green** — Green of pixel 94.

**392 · Pixel 94 blue** — Blue of pixel 94.

**393 · Pixel 95 dim** — Dimmer of pixel 95.

**394 · Pixel 95 red** — Red of pixel 95.

**395 · Pixel 95 green** — Green of pixel 95.

**396 · Pixel 95 blue** — Blue of pixel 95.

**397 · Pixel 96 dim** — Dimmer of pixel 96.

**398 · Pixel 96 red** — Red of pixel 96.

**399 · Pixel 96 green** — Green of pixel 96.

**400 · Pixel 96 blue** — Blue of pixel 96.
