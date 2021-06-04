# Stop Time and Speed Calculation from Gmap Data

## Problem
$$
\begin{align*}
t &= \frac{d}{a} + b*s \\
where\\
t &= time \\
d &= distance \\
s &= stops \\
a &= speed \ (unknown\ constant)\\
b &= stopTime \ (unknown\ constant)
\end{align*}
$$

## Solution
Let the 2 known points be A and B.
To solve for a and b for point A:
$$
\begin{align*}
t_A - \frac{d_A}{a} &= b * s_A\\
b &= \frac{t_A - \frac{d_A}{a}}{s_A}
\end{align*}
$$

Substitution of $b$ into original eqn for point B:
$$
\begin{align*}
t_B &= \frac{d_B}{a} + b*s_B\\
&= \frac{d_B}{a} + (\frac{t_A - \frac{d_A}{a}}{s_A}) * s_B \\
&= \frac{d_B}{a} + \frac{s_Bt_A}{s_A} -\frac{d_As_B}{s_Aa}\\
t_B - \frac{s_Bt_A}{s_A} &= \frac{d_B}{a} -\frac{d_As_B}{s_Aa}\\
&= \frac{1}{a}(d_B -\frac{d_As_B}{s_A})\\
a &= \frac{d_B -\frac{s_Bd_A}{s_A}}{t_B - \frac{s_Bt_A}{s_A}}
\end{align*}
$$

To find $b$, substitute $a$ back into the equation for $b$

### Checking
Using the following input as sample:
```
{stops=32, distance=39.081000 km, time=66.000000 min}
{stops=31, distance=36.573000 km, time=62.000000 min}
```
Answer should be:
```
a = speed = 0.66411...
b = stopTime = 0.2235337...
```

### Error Handling
If any of the 2 data points return "null", only one data point can be used. To handle this, the stopTime (b) is assumed to be a default value.
$$
a = \frac{d}{t-bs}
$$

## Coding
### Final Equations
$$
\begin{align*}
a &= \frac{d_B -\frac{s_Bd_A}{s_A}}{t_B - \frac{s_Bt_A}{s_A}}\\
b &= \frac{t_A - \frac{d_A}{a}}{s_A}
\end{align*}
$$	

### Optimization
To optimize calculations, number of divisions should be decreased. This is because based on [Intel's arithmetic latencies](https://www.agner.org/optimize/instruction_tables.pdf#page=63). The following equations and corresponding pseudocode is proposed. In the pseudocode, `speed` refers to the constant `a` while `stopTime` refers to the constant `b`, and `a` and `b` refer to the 2 data points.

$$
\begin{align*}
s_{ratio} &= \frac{s_B}{s_A}\\
a &= \frac{d_B -s_{ratio}d_A}{t_B - s_{ratio}t_A}\\
b &= \frac{t_Aa - d_A}{s_Aa}
\end{align*}
$$	

```
sRatio = b.stops / a.stops
speed = (b.distance - sRatio*a.distance)/(b.time - sRatio*a.time)
stopTime = (a.time*speed - a.distance)/(a.stops * speed)
```
