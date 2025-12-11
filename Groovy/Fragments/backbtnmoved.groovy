<!-- location-- servicedest.portal.header -->
 def render() {
  writer.write("""
    <style>
      #backBtn {
        font-size: 13px;
        padding: 0 16px 0 32px;
        height: 34px;
        line-height: 34px;
        text-transform: uppercase;
        display: inline-block;
        border-radius: 20px;
        font-weight: 600;
        cursor: pointer;
        text-decoration: none;
        position: absolute;
        right: 12px;
        top: 20%;
        background: linear-gradient(180deg, #0052cc 0%, #0747a6 100%);
        color: #fff;
        box-shadow: 0 2px 4px rgba(0,0,0,0.15);
        transition: opacity 0.3s ease;
        opacity: 0;          /* invisible until shown */
      }
      #backBtn.show {
        opacity: 1;
      }
      #backBtn:hover {
        background: linear-gradient(180deg, #0065ff 0%, #0052cc 100%);
        box-shadow: 0 4px 8px rgba(0,0,0,0.2);
      }
      #backBtn:active {
        background: #0747a6;
        transform: scale(0.97);
      }
      #backBtn::before {
        content: "?";
        font-size: 16px;
        position: absolute;
        left: 12px;
        top: 50%;
        transform: translateY(-50%);
        color: white;
      }
    </style>

    <script>
      (function(){
        var issueViewRegex = /portal\\/\\d+\\/[A-Z]+-\\d+/i;

        function injectBackBtn(){
          var url = window.location.href;
          if (!issueViewRegex.test(url)) return;

          var header = document.getElementById("rw_request_header");
          if (!header) return; // header not ready

          if (!document.getElementById("backBtn")) {
            // Create element with correct class from the start
            var icon = document.createElement("a");
            icon.id = "backBtn";
            icon.href = "javascript:window.history.back();";
            icon.textContent = "Back";
            icon.classList.add("show");
            header.append(icon);
          }
        }

        // Use MutationObserver so we only inject when header is in DOM
        var observer = new MutationObserver(function(muts, obs){
          if (document.getElementById("rw_request_header")) {
            injectBackBtn();
          }
        });
        observer.observe(document.body, { childList: true, subtree: true });

        // Handle SPA URL changes
        var last = location.href;
        setInterval(function(){
          if (location.href !== last) {
            last = location.href;
            injectBackBtn();
          }
        }, 500);
      })();
    </script>
  """)
}
render()
