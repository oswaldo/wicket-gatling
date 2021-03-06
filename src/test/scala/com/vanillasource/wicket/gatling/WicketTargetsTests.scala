/**
  * Copyright (C) 2014 VanillaSource
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */

package com.vanillasource.wicket.gatling

import org.scalatest.WordSpec

class WicketTargetsTests extends WordSpec {
   "Target extraction" should {
      "return empty list if no urls are contained in body" in {
         val target = WicketTargets("/", "body")

         assert(target.targets.isEmpty)
      }
      "detect direct links in link tags" in {
         val target = WicketTargets("/", 
             """<a wicketpath="path" href="./com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link" class="list-group-item">
                <i class="glyphicon glyphicon-play"></i>&nbsp;Sample Link</a>""")

         assert(target.targets(0) == (TargetSpec(TargetType.Link, List("path")), 
            "/com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link"))
      }
      "detect reverse direct links in link tags" in {
         val target = WicketTargets("/", 
             """<a href="./com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link" wicketpath="path" class="list-group-item">
                <i class="glyphicon glyphicon-play"></i>&nbsp;Sample Link</a>""")

         assert(target.targets(0) == (TargetSpec(TargetType.Link, List("path")),
            "/com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link"))
      }
      "detect forms" in {
         val target = WicketTargets("/", 
             """<form id="sample_form26" wicketpath="path" method="post" action="./com.vanillasource.wicket.gatling.SampleForm?2-1.IFormSubmitListener-sample~panels-panels-0-sample~form">""")

         assert(target.targets(0) == (TargetSpec(TargetType.Form, List("path")),
            "/com.vanillasource.wicket.gatling.SampleForm?2-1.IFormSubmitListener-sample~panels-panels-0-sample~form"))
      }
      "detect onclick handlers in any tag" in {
         val target = WicketTargets("/", 
             """<div wicketpath="path" onclick="var win = this.ownerDocument.defaultView || this.ownerDocument.parentWindow; if (win == window) { window.location.href=&#039;./?2-1.ILinkListener-subject~tab~bar-panel-panel-list~view~container-tile-hover~wrapper-content~row-content~col-content&amp;param1=12345&#039;; } ;return false">""")

         assert(target.targets(0) == (TargetSpec(TargetType.Link, List("path")),
            "/?2-1.ILinkListener-subject~tab~bar-panel-panel-list~view~container-tile-hover~wrapper-content~row-content~col-content&param1=12345"))
      }
      "deconstruct wicket path into path components" in {
         val target = WicketTargets("/", 
             """<a wicketpath="path-component-1_path-component-2_path-component-3" href="./com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link" class="list-group-item">
                <i class="glyphicon glyphicon-play"></i>&nbsp;Sample Link</a>""")

         assert(target.targets(0)._1.path == List("path-component-1", "path-component-2", "path-component-3"))
      }
      "preserve ordering of same type links in body" in {
         val target1 = WicketTargets("/", 
             """<a wicketpath="path_1" href="./com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link" class="list-group-item">
                <i class="glyphicon glyphicon-play"></i>&nbsp;Sample Link</a>"""+
             """<a wicketpath="path_2" href="./com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link" class="list-group-item">
                <i class="glyphicon glyphicon-play"></i>&nbsp;Sample Link</a>""")

         val target2 = WicketTargets("/", 
             """<a wicketpath="path_2" href="./com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link" class="list-group-item">
                <i class="glyphicon glyphicon-play"></i>&nbsp;Sample Link</a>"""+
             """<a wicketpath="path_1" href="./com.vanillasource.wicket.gatling.SamplePage?1-1.ILinkListener-right~panel-right~panel~content-link-launch~Sample+Link" class="list-group-item">
                <i class="glyphicon glyphicon-play"></i>&nbsp;Sample Link</a>""")

         assert(target1.targets(0)._1.path == List("path", "1"))
         assert(target1.targets(1)._1.path == List("path", "2"))
         assert(target2.targets(0)._1.path == List("path", "2"))
         assert(target2.targets(1)._1.path == List("path", "1"))
      }
      "escape html encoded characters in uris" in {
         val target = WicketTargets("/", """<a wicketpath="path" href="/some&#243;and&amp;"></a>""")

         assert(target.targets(0)._2 == "/someóand&")
      }
      "apply found uris on the request uri supplied" in {
         val target = WicketTargets("/wicket/SomePage?1", 
            """<a wicketpath="path" href="./SomePage?1-1.ILinkListener-something"></a>""")

         assert(target.targets(0)._2 == "/wicket/SomePage?1-1.ILinkListener-something")
      }
      "detect ajax links" in {
         val target = WicketTargets("/wicket/SomePage?1",
            """Wicket.Ajax.ajax({"u":"./?0-8.IBehaviorListener.0-ajaxlink","e":"click","c":"ajaxlink3"});;"""+
            """<a wicket:id="ajaxlink" id="ajaxlink3" wicketpath="ajaxlink" href="javascript:;">Ajax Link</a>""")

         assert(target.targets(0) == (TargetSpec(TargetType.Link, List("ajaxlink")), 
            "/wicket/?0-8.IBehaviorListener.0-ajaxlink"))
      }
      "detect ajax forms" in {
         val target = WicketTargets("/wicket/SomePage?1",
            """Wicket.Ajax.ajax({"f":"ajaxform1","u":"./?0-8.IBehaviorListener.0-ajaxform-ajaxformbutton","e":"click","c":"ajaxformbutton2","sc":"ajaxformbutton","m":"POST"});;"""+
            """<button wicket:id="ajaxformbutton" name="ajaxformbutton" id="ajaxformbutton2" wicketpath="ajaxform_ajaxformbutton">Submit</button>""")

         assert(target.targets(0) == (TargetSpec(TargetType.Form, List("ajaxform", "ajaxformbutton")), 
            "/wicket/?0-8.IBehaviorListener.0-ajaxform-ajaxformbutton"))
      }
   }

   "Matching a target spec" when {
      "first component is supplied" should {
         "match" in {
            assert(testSpec.matches("A"))
         }
      }
      "last component is supplied" should {
         "match" in {
            assert(testSpec.matches("E"))
         }
      }
      "not contained component is supplied" should {
         "not match" in {
            assert(!testSpec.matches("F"))
         }
      }
      "full path is supplied" should {
         "match" in {
            assert(testSpec.matches("A", "B", "C", "D", "E"))
         }
      }
      "part of path is supplied" should {
         "match" in {
            assert(testSpec.matches("A", "D", "E"))
         }
      }
      "empty query is supplied " should {
         "match" in {
            assert(testSpec.matches())
         }
      }
      "reversed components are supplied" should {
         "not match" in {
            assert(testSpec.matches("D", "A"))
         }
      }
      "both the right type and component is supplied" should {
         "match" in {
            assert(testSpec.matches(TargetType.Link, "A"))
         }
      }
      "wrong type and right component is supplied" should {
         "not match" in {
            assert(!testSpec.matches(TargetType.Form, "A"))
         }
      }
   }

   "Selecting from targets" should {
      "return matching targets" in {
         val targets = WicketTargets(List(
            (TargetSpec(TargetType.Link, List("A", "B")), "URI1"),
            (TargetSpec(TargetType.Link, List("A", "C")), "URI2"))) ("", "")

         assert(targets.getUris(TargetType.Link, "A") == List("URI1", "URI2"))
      }
      "ommit non-matching targets" in {
         val targets = WicketTargets(List(
            (TargetSpec(TargetType.Link, List("A", "B")), "URI1"),
            (TargetSpec(TargetType.Link, List("A", "C")), "URI2"))) ("", "")

         assert(targets.getUris(TargetType.Link, "C") == List("URI2"))
      }
   }

   private def testSpec = TargetSpec(TargetType.Link, List("A", "B", "C", "D", "E"))
}

