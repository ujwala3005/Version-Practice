//com.atlassian.jira.plugin.headernav.left.context
writer.write("""
<style>
button[title='Click to login with MyCotiviti using Okta SSO'] {
    background: #30006e !important;
    color: white !important;
    margin-top: 10% !important;
    margin-left: 0% !important;
}
</style>

<script>
document.addEventListener("DOMContentLoaded", function() {
    const btn = document.querySelector("button[title='Click to login with MyCotiviti using Okta SSO']");
    if (btn) {

        // Remove all existing click handlers by cloning the node
        const newBtn = btn.cloneNode(true);
        btn.parentNode.replaceChild(newBtn, btn);

        // FORCE override redirect
        newBtn.addEventListener("click", function(event) {
            event.stopImmediatePropagation();  // Stop other listeners
            event.preventDefault();            // Stop default actions
            window.location.href = "https://login-preview.cotiviti.com/app/cotiviti-ext_mycotivitipreprod_1/exk1r2c2wwgVsnTpa0h8/sso/saml";  // Final redirect
        }, true); // capture = true ensures it fires before others
    }
});
</script>
""")
