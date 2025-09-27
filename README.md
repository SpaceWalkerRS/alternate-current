
<div align="right">
  <details>
    <summary >🌐 Language</summary>
    <div>
      <div align="center">
        <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=en">English</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=zh-CN">简体中文</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=zh-TW">繁體中文</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=ja">日本語</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=ko">한국어</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=hi">हिन्दी</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=th">ไทย</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=fr">Français</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=de">Deutsch</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=es">Español</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=it">Italiano</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=ru">Русский</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=pt">Português</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=nl">Nederlands</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=pl">Polski</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=ar">العربية</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=fa">فارسی</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=tr">Türkçe</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=vi">Tiếng Việt</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=id">Bahasa Indonesia</a>
        | <a href="https://openaitx.github.io/view.html?user=SpaceWalkerRS&project=alternate-current&lang=as">অসমীয়া</
      </div>
    </div>
  </details>
</div>

# Alternate Current

Alternate Current is an efficient and non-locational redstone dust implementation. Its main focus lies in reducing the lag caused by redstone dust, by optimizing the power calculations and reducing the number of shape and block updates emitted. As a side effect of these changes the block update order of redstone dust networks is predictable and intuitive rather than locational and chaotic.

## Performance

MSPT contributions of redstone dust are up to 20 times lower with Alternate Current, all the while maintaining a high level of Vanilla parity. Its low number of code modifications make it minimally invasive, so it is an easy drop-in replacement for Vanilla redstone dust.

## How does it work?

The algorithm Alternate Current uses was designed with the following goals in mind:
1. Minimize the number of times a wire checks its surroundings to determine its power level.
2. Minimize the number of block and shape updates emitted.
3. Emit block and shape updates in a deterministic, non-locational order, fixing bug MC-11193.

In Vanilla redstone wire is laggy because it fails on points 1 and 2.

Redstone wire updates recursively and each wire calculates its power level in isolation rather than in the context of the network it is a part of. This means a wire in a grid can change its power level over half a dozen times before settling on its final value. This problem used to be worse in 1.13 and below, where a wire would only decrease its power level by 1 at a time.

In addition to this, a wire emits 42 block updates and up to 22 shape updates each time it changes its power level.

Of those 42 block updates, 6 are to itself, which are thus not only redundant, but a big source of lag, since those cause the wire to unnecessarily re-calculate its power level. A block only has 24 neighbors within a Manhattan distance of 2, meaning 12 of the remaining 36 block updates are duplicates and thus also redundant.

Of the 22 shape updates, only 6 are strictly necessary. The other 16 are sent to blocks diagonally above and below. These are necessary if a wire changes its connections, but not when it changes its power level.

Redstone wire in Vanilla also fails on point 3, though this is more of a quality-of-life issue than a lag issue. The recursive nature in which it updates, combined with the location-dependent order in which each wire updates its neighbors, makes the order in which neighbors of a wire network are updated incredibly inconsistent and seemingly random.

Alternate Current fixes each of these problems as follows.

(1)
To make sure a wire calculates its power level as little as possible, we remove the recursive nature in which redstone wire updates in Vanilla. Instead, we build a network of connected wires, find those wires that receive redstone power from "outside" the network, and spread the power from there. This has a few advantages:

- Each wire checks for power from non-wire components at most once, and from nearby wires just twice.
- Each wire only sets its power level in the world once. This is important, because calls to Level.setBlock are even more expensive than calls to Level.getBlockState.

(2)
There are 2 obvious ways in which we can reduce the number of block and shape updates.

- Get rid of the 18 redundant block updates and 16 redundant shape updates, so each wire only emits 24 block updates and 6 shape updates whenever it changes its power level.
- Only emit block updates and shape updates once a wire reaches its final power level, rather than at each intermediary stage.

For an individual wire, these two optimizations are the best you can do, but for an entire grid, you can do better!

Since we calculate the power of the entire network, sending block and shape updates to the wires in it is redundant. Removing those updates can reduce the number of block and shape updates by up to 20%.

(3)
To make the order of block updates to neighbors of a network deterministic, the first thing we must do is to replace the location- dependent order in which a wire updates its neighbors. Instead, we base it on the direction of power flow. This part of the algorithm was heavily inspired by theosib's 'RedstoneWireTurbo', which you can read more about in theosib's comment on Mojira [here](https://bugs.mojang.com/browse/MC-81098?focusedCommentId=420777&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-420777) or by checking out its implementation in carpet mod [here](https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/helpers/RedstoneWireTurbo.java).

The idea is to determine the direction of power flow through a wire based on the power it receives from neighboring wires. For example, if the only power a wire receives is from a neighboring wire to its west, it can be said that the direction of power flow through the wire is east.

We make the order of block updates to neighbors of a wire depend on what is determined to be the direction of power flow. This not only removes locationality entirely, it even removes directionality in a large number of cases. Unlike in 'RedstoneWireTurbo', however, I have decided to keep a directional element in ambiguous cases, rather than to introduce randomness, though this is trivial to change.

While this change fixes the block update order of individual wires, we must still address the overall block update order of a network. This turns out to be a simple fix, because of a change we made earlier: we search through the network for wires that receive power from outside it, and spread the power from there. If we make each wire transmit its power to neighboring wires in an order dependent on the direction of power flow, we end up with a non-locational and largely non-directional wire update order.
