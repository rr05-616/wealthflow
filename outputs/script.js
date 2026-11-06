const replayButton = document.querySelector('#replay');
const calculationCard = document.querySelector('.calculation-card');
const amountInput = document.querySelector('#total-amount');
const weights = [33333, 33333, 33334];
const formatRupees = (paise) => `₹${(paise / 100).toFixed(2)}`;

function updateAllocation() {
  if (!amountInput) return;
  const totalPaise = Math.max(1, Math.round(Number(amountInput.value || 0) * 100));
  const exactNumerators = weights.map((weight) => totalPaise * weight);
  const floors = exactNumerators.map((value) => Math.floor(value / 100000));
  const remainders = exactNumerators.map((value) => value % 100000);
  const residual = totalPaise - floors.reduce((sum, value) => sum + value, 0);
  const order = [0, 1, 2].sort((a, b) => remainders[b] - remainders[a] || a - b);
  const finalValues = [...floors];
  for (let i = 0; i < residual; i++) finalValues[order[i % order.length]]++;

  document.querySelectorAll('[data-exact]').forEach((node) => {
    const index = Number(node.dataset.exact);
    const milliRupees = Math.round(exactNumerators[index] / 10000);
    node.textContent = `₹${(milliRupees / 1000).toFixed(3)}`;
  });
  document.querySelectorAll('[data-floor]').forEach((node) => node.textContent = formatRupees(floors[Number(node.dataset.floor)]));
  document.querySelectorAll('[data-final]').forEach((node) => node.textContent = formatRupees(finalValues[Number(node.dataset.final)]));
  document.querySelectorAll('[data-winner]').forEach((node) => node.textContent = `+₹${(residual / 100).toFixed(2)} to ${['A', 'B', 'C'][order[0]]}`);
  document.querySelector('#residual-amount').textContent = `+ ${formatRupees(residual)}`;
  document.querySelector('#reconciled-total').textContent = formatRupees(totalPaise);
}

amountInput?.addEventListener('input', updateAllocation);
amountInput?.addEventListener('change', () => {
  const value = Math.max(0.01, Math.min(99999999.99, Number(amountInput.value) || 0.01));
  amountInput.value = value.toFixed(2);
  updateAllocation();
});
updateAllocation();

replayButton?.addEventListener('click', () => {
  calculationCard.classList.remove('replay');
  replayButton.classList.remove('spinning');
  void calculationCard.offsetWidth;
  calculationCard.classList.add('replay');
  replayButton.classList.add('spinning');
  window.setTimeout(() => {
    calculationCard.classList.remove('replay');
    replayButton.classList.remove('spinning');
  }, 1800);
});

const revealObserver = new IntersectionObserver((entries) => {
  for (const entry of entries) {
    if (entry.isIntersecting) {
      entry.target.classList.add('is-visible');
      revealObserver.unobserve(entry.target);
    }
  }
}, { threshold: 0.12 });

document.querySelectorAll('.work-module, .architecture-module').forEach((section) => revealObserver.observe(section));
