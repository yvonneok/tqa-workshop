/*
 * Copyright 2016-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.tqaworkshop.chapter3

import java.io.File

import scala.collection.immutable

import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrlInstance
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import net.sf.saxon.s9api.Processor

/**
 * Like `ShowXbrlInstanceElemAsMatrix` in chapter 1, but using class `XbrlInstanceToRowsConverter`.
 *
 * @author Chris de Vreeze
 */
object ShowXbrlInstanceAsMatrix {

  def main(args: Array[String]): Unit = {
    require(args.size <= 1, s"Usage: ShowXbrlInstanceAsMatrix [ <XML input file path> ]")

    val inputXmlFile =
      if (args.isEmpty) {
        val inputFileUri = classOf[ShowXbrlInstanceAsMatrix].getResource("/sample-Instance-Proof.xml").toURI
        new File(inputFileUri)
      } else {
        new File(args(0))
      }

    val processor = new Processor(false)
    val docBuilder = new SaxonDocumentBuilder(processor.newDocumentBuilder, (uri => uri))

    // We could have used an entirely different DocumentBuilder.
    // The converter could not care less which XML implementation is used underneath (but we do care).
    val rootElem: BackingElemApi = docBuilder.build(inputXmlFile.toURI)
    val xbrlInstance: XbrlInstance = XbrlInstance(rootElem)

    val converter = new XbrlInstanceToRowsConverter

    val matrix: immutable.IndexedSeq[XbrlInstanceToRowsConverter.Row] =
      converter.convertXbrlInstance(xbrlInstance)

    val dimensions: immutable.IndexedSeq[EName] =
      matrix.flatMap(_.explicitDimensionMembers.keySet).distinct.sortBy(_.toString)

    val headerLine: immutable.IndexedSeq[String] =
      immutable.IndexedSeq(
        "Path",
        "Concept",
        "Scheme",
        "Identifier",
        "Instant",
        "Start date",
        "End date") ++ dimensions.map(_.toString) :+ "Measures"

    val detailLines: immutable.IndexedSeq[immutable.IndexedSeq[String]] =
      matrix map { row =>
        immutable.IndexedSeq[String](
          row.path.toResolvedCanonicalXPath,
          row.conceptName.toString,
          row.identifierScheme,
          row.identifierValue,
          row.periodInstantOption.map(_.toString).getOrElse(""),
          row.periodStartDateOption.map(_.toString).getOrElse(""),
          row.periodEndDateOption.map(_.toString).getOrElse("")) ++
          (dimensions.map(dim => row.explicitDimensionMembers.get(dim).map(_.toString).getOrElse(""))) :+
          row.measures.mkString(", ")
      }

    val separator = System.getProperty("fieldSeparator", ";")

    println(headerLine.mkString(separator))

    for (detailLine <- detailLines) {
      println(detailLine.mkString(separator))
    }
  }
}

class ShowXbrlInstanceAsMatrix
