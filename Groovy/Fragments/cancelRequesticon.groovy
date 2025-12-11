//atl.header
def render() {
    writer.write("""
<style>
.TestRenew2 {
    margin-top: 10px;
    cursor: pointer;
}

li {
    list-style-type: none;
    margin: 0;
    padding: 0;
}

input[type="date"] {
    width: 90%;
    margin-bottom: 10px;
  	border: 2px solid #000; 
  	border-radius: 5px;
}

textarea {
    width: 90%;
    margin-bottom: 10px;
    height: 100px;
  	border: 2px solid #000; 
  	border-radius: 5px;
}

</style>
<script>
document.addEventListener('DOMContentLoaded', function() {
(function () {
  // Only run on portal URLs like desk/portal/7/IT-xxxxx
  if (!window.location.pathname.match(/desk\\/portal\\/7\\/IT/i)) return;

  console.log("ScriptRunner: Renew Access logic active");

  function getContextPath() {
    try {
      if (window.AJS && typeof AJS.contextPath === 'function') return AJS.contextPath();
    } catch {}
    return '';
  }

  // Helper: wait for DOM elements
  function waitFor(fn, { interval = 250, timeout = 10000 } = {}) {
    return new Promise((resolve, reject) => {
      const start = Date.now();
      (function loop() {
        try {
          const val = fn();
          if (val) return resolve(val);
        } catch {}
        if (Date.now() - start > timeout) return reject("timeout");
        setTimeout(loop, interval);
      })();
    });
  }

  // Inject Renew Access button
  function addRenewUI() {
    const status = document.querySelector('.rw_request_status_container');
    if (!status || !status.textContent.includes('Request Resolved')) return;

    const parent = document.querySelector('.rw_request_actions');
    if (!parent) return;

    // Hide original renew
    const orig = document.querySelector('.js-trigger-customer-transition');
    if (orig) orig.style.display = 'none';

    if (parent.querySelector('.TestRenew')) return; // already added

    const img = document.createElement('img');
    img.src = getContextPath() + '/plugins/cute/static/resource/PO/Renew_Access_img.png';
    img.style.cssText = "margin-top:10px;opacity:0.8;max-width:24px;margin-left:12px;";

    const Renewbtn = document.createElement('button');
    Renewbtn.textContent = 'Renew Access';
    Renewbtn.className = 'TestRenew';
    Renewbtn.style.cssText = 'margin-left:12px;line-height:44px;height:44px;color:#111;border:none;cursor:pointer;';

    const li = document.createElement('li');
    li.className = 'listItem';
    li.style.cssText = 'display:flex;align-items:center;list-style-type:none;padding:0;';
    li.appendChild(img);
    li.appendChild(Renewbtn);
    parent.appendChild(li);

    Renewbtn.addEventListener('click', function() {
      Renewbtn.disabled = true;

      const box = document.createElement('div');
      box.style.cssText = 'border:2px solid #000;padding:20px;margin-top:20px;border-radius:10px;width:320px;background:#f9f9f9;';

      const label = document.createElement('label');
      label.textContent = 'Please enter New Expiration date below* :';

      const input = document.createElement('input');
      input.type = 'date';
      input.required = true;

      const label1 = document.createElement('label');
      label1.style.marginTop = '14px';
      label1.textContent = 'Please enter the comments* :';

      const comment = document.createElement('textarea');
      comment.required = true;

      const Renew = document.createElement('button');
      Renew.textContent = 'Renew';
      Renew.style.cssText = 'margin-top:14px;width:100px;height:30px;border-radius:5px;cursor:pointer;';

      const Cancel = document.createElement('button');
      Cancel.textContent = 'Cancel';
      Cancel.style.cssText = 'margin-top:14px;width:100px;height:30px;border-radius:5px;margin-left:100px;cursor:pointer;';

      const buttonContainer = document.createElement('div');
      buttonContainer.style.cssText = 'display:flex;margin-top:14px;';
      buttonContainer.appendChild(Renew);
      buttonContainer.appendChild(Cancel);

      const li1 = document.createElement('li');
      li1.style.cssText = 'display:flex;flex-direction:column;align-items:flex-start;list-style-type:none;padding:0;font-family:Arial,sans-serif;';
      li1.appendChild(label);
      li1.appendChild(input);
      li1.appendChild(label1);
      li1.appendChild(comment);
      li1.appendChild(buttonContainer);

      box.appendChild(li1);
      parent.appendChild(box);

      let inputDate = '';
      let userComments = '';

      input.addEventListener('input', () => { inputDate = input.value; });
      comment.addEventListener('input', () => { userComments = comment.value; });

      Cancel.addEventListener('click', () => {
        box.remove();
        Renewbtn.disabled = false;
      });

      Renew.addEventListener('click', () => {
        Renew.disabled = true;

        const oldErr = box.querySelector('.errorMessageClass');
        if (oldErr) oldErr.remove();

        const err = document.createElement('label');
        err.className = 'errorMessageClass';
        err.style.color = 'red';
        err.style.marginTop = '10px';

        const today = new Date(); today.setHours(0,0,0,0);

        if (!inputDate || !userComments) {
          err.textContent = '*Please fill the mandatory fields';
          box.appendChild(err);
          Renew.disabled = false;
          return;
        }

        if (new Date(inputDate) < today) {
          err.textContent = '*Renewal Expiration Date should be Future Date!!!';
          box.appendChild(err);
          Renew.disabled = false;
          return;
        }

        const token = 'token';
        const ticketID = window.location.pathname.split('/').pop();
        const API_url = 'https://domain.com' + ticketID + '/transitions';
        const updatedData = {
          transition: { id: '991' },
          fields: { customfield_13219: inputDate },
          update: { comment: [{ add: { body: userComments } }] }
        };

        console.log('Renew API:', API_url, updatedData);

        fetch(API_url, {
          method: 'POST',
          headers: {
            Authorization: 'Bearer ' + token,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(updatedData)
        })
        .then(r => r.text())
        .then(t => { console.log('Renew success:', t); location.reload(); })
        .catch(e => { console.error('Renew error:', e); Renew.disabled = false; });
      });
    });
  }

  // Watch and trigger init after DOM ready
  document.addEventListener('DOMContentLoaded', function() {
    const observer = new MutationObserver(() => {
      try { addRenewUI(); } catch (e) { console.warn('Mutation error:', e); }
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
    setTimeout(addRenewUI, 1500);
  });
})();
</script>
""")
}
render()
