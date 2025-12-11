//servicedesk.portal.footer
def render() {
  writer.write("""
    <style>
      #cp-right-rail {
        position: fixed; top: 10px; right: 10px;
        width: 36vw; max-width: 720px; min-width: 300px;
        height: auto; max-height: calc(100vh - 40px);
        background: transparent; /* make parent transparent */
        border: none; box-shadow: none;
        z-index: 1000;
        display: none; /* hidden by default */
        font-family: Arial, sans-serif;
      }
      #cp-right-rail .rail-card {
        background: #fff;
        /*border: 1px solid #e5e7eb;*/
        border-radius: 10px;
        /*box-shadow: 0 8px 30px rgba(0,0,0,.08);*/
        overflow: hidden;
        margin-bottom: 16px;
      }
      #cp-right-rail img {
        width: 100%; height: auto; object-fit: contain;
      }
      #cheatSheet {
        border: 1px solid #e5e7eb;
        border-radius: 10px;
        padding: 14px;
        font-size: 14px;
        background: #fff;
        box-shadow: 0 4px 20px rgba(0,0,0,.06);
        display: none; /* only for selected IDs */
      }
      #cheatSheet img {
        width: 20px; vertical-align: middle; margin-right: 8px;
      }
      #cheatSheet span {
        font-size: 16px; font-weight: 500; color: #333;
      }
      #cheatSheet a {
        color: #0052cc; font-weight: 600; text-decoration: none;
      }
      #cheatSheet a:hover { text-decoration: underline; }
      @media (max-width: 1200px) { #cp-right-rail { display: none !important; } }
    </style>

    <div id="cp-right-rail">
      <div class="rail-card">
        <img id="cp-rail-img" src="" alt="Request Visual"/>
      </div>
      <div id="cheatSheet" class="rail-card">
        <img src="/plugins/servlet/desk/resource/global/imagebank/images/cheatSheetbg.png" alt="Info"/>
        <span>
          For more information related to Security Services<br>
          <a href="/plugins/servlet/desk/site/global/article/31490537/Security+Services+Cheat+Sheet" target="_blank">Click Here</a>
        </span>
      </div>
    </div>

    <script>
      (function(){
        var IMAGES = {
          "109": "/plugins/servlet/desk/resource/global/imagebank/images/PHInstructions.png",
          "166": "/plugins/servlet/desk/resource/global/imagebank/images/software.png",
          "174": "/plugins/servlet/desk/resource/global/imagebank/images/infrabanner2.png",
          "175": "/plugins/servlet/desk/resource/global/imagebank/images/hardware.png",
          "176": "/plugins/servlet/desk/resource/global/imagebank/images/security2.png",
          "177": "/plugins/servlet/desk/resource/global/imagebank/images/GSR2.png",
          "178": "/plugins/servlet/desk/resource/global/imagebank/images/facility.png",
          "179": "/plugins/servlet/desk/resource/global/imagebank/images/AccIdBanner3.png",
          "479": "/plugins/servlet/desk/resource/global/imagebank/images/security.png"
        };

        var CHEATSHEET_IDS = new Set(["479"]);

        function togglePanel(){
          var rail = document.getElementById('cp-right-rail');
          var img  = document.getElementById('cp-rail-img');
          var cheat = document.getElementById('cheatSheet');
          if (!rail || !img || !cheat) return;

          var m = window.location.href.match(/create\\/(\\d+)/);
          if (m && IMAGES[m[1]]) {
            img.src = IMAGES[m[1]];
            rail.style.display = 'block';
            cheat.style.display = CHEATSHEET_IDS.has(m[1]) ? 'block' : 'none';
          } else {
            rail.style.display = 'none';
            img.src = "";
            cheat.style.display = 'none';
          }
        }

        function applyPortalTweaks(){
          if (window.location.href.indexOf("portal/7/") !== -1) {
            AJS.\$("#content-wrapper").css({ margin: "0", width: "65%" });
            setTimeout(function () { AJS.\$("span.vp-optional").css("display", "none"); }, 10);
            setTimeout(function () {
              AJS.\$('label:contains("None")').css("display", "none");
              AJS.\$("span.vp-optional").css("display", "none");
            }, 50);
          }
        }

        togglePanel();
        applyPortalTweaks();

        var last = location.href;
        setInterval(function(){
          if (location.href !== last) {
            last = location.href;
            togglePanel();
            applyPortalTweaks();
          }
        }, 500);
      })();
    </script>
  """)
}
render()
