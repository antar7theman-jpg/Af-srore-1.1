package com.example

import com.example.data.models.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun product_purchasePrice_and_profit_isCorrect() {
    val product = Product(
      name = "سكر",
      purchasePrice = 20.0,
      price = 25.0,
      stock = 10
    )
    assertEquals(20.0, product.purchasePrice, 0.001)
    assertEquals(25.0, product.price, 0.001)
    assertEquals(5.0, product.profitPerUnit, 0.001)
    assertEquals(25.0, product.profitMarginPercent, 0.001)
  }
}
