def createComplete3DShape(center, radius, height, segments) = {
  particles = []

  // 圆柱体部分
  for y in 0..&height {
      for angle in 0..&segments {
          a = &angle * 2 * 3.14159 / &segments
          x = &center::x() + &radius * cos(&a)
          z = &center::z() + &radius * sin(&a)
          yPos = &center::y() + &y

          loc = location(&x, &yPos, &z)
          &particles::add(["REDSTONE", &loc])
      }
  }

  // 顶部半球形
  sphereRadius = &radius
  for phi in 0..(&segments/2) {
      for theta in 0..&segments {
          p = &phi * 3.14159 / (&segments/2)
          t = &theta * 2 * 3.14159 / &segments

          x = &center::x() + &sphereRadius * sin(&p) * cos(&t)
          z = &center::z() + &sphereRadius * sin(&p) * sin(&t)
          y = &center::y() + &height + &sphereRadius * cos(&p)

          loc = location(&x, &y, &z)
          &particles::add(["REDSTONE", &loc])
      }
  }

  // 底部左侧球形
  leftBallCenter = location(&center::x() - &radius*0.8, &center::y() - &radius*0.8, &center::z())
  for phi in 0..&segments {
      for theta in 0..&segments {
          p = &phi * 3.14159 / &segments
          t = &theta * 2 * 3.14159 / &segments

          x = &leftBallCenter::x() + &radius*0.8 * sin(&p) * cos(&t)
          z = &leftBallCenter::z() + &radius*0.8 * sin(&p) * sin(&t)
          y = &leftBallCenter::y() + &radius*0.8 * cos(&p)

          loc = location(&x, &y, &z)
          &particles::add(["REDSTONE", &loc])
      }
  }

  // 底部右侧球形
  rightBallCenter = location(&center::x() + &radius*0.8, &center::y() - &radius*0.8, &center::z())
  for phi in 0..&segments {
      for theta in 0..&segments {
          p = &phi * 3.14159 / &segments
          t = &theta * 2 * 3.14159 / &segments

          x = &rightBallCenter::x() + &radius*0.8 * sin(&p) * cos(&t)
          z = &rightBallCenter::z() + &radius*0.8 * sin(&p) * sin(&t)
          y = &rightBallCenter::y() + &radius*0.8 * cos(&p)

          loc = location(&x, &y, &z)
          &particles::add(["REDSTONE", &loc])
      }
  }

  &particles
}

playerPos = &audience::location()
particles = createComplete3DShape(&playerPos, 2, 6, 20)